package me.theclashfruit.kovr.client.menu.xml;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.theclashfruit.kovr.Kovr;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.*;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.fs.ResourceFileSystem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class MenuParser {
    private final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
    private final TextRenderer textRenderer;

    private final Map<String, Widget> widgets = new HashMap<>();

    public MenuParser(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public Widget parseForFile(Identifier id) {
        try {
            if (resourceManager.getResource(id).isPresent()) {
                Resource resource = resourceManager.getResource(id).orElseThrow();
                try (InputStream input = resource.getInputStream()) {
                    Document doc = parseXML(input);

                    Element root = doc.getDocumentElement();
                    Widget rootWidget = getWidgetByName(root.getNodeName(), root.getAttributes(), root.getTextContent());

                    buildWidgets(root, rootWidget);
                    return rootWidget;
                }
            } else {
                System.out.println("Resource not found: " + id);
            }
        } catch (Exception e) {
            Kovr.LOGGER.error("Error parsing XML file: {}", id, e);
        }

        return null;
    }

    private Document parseXML(InputStream xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(xml);
    }

    private Widget getWidgetByName(String n, NamedNodeMap attributes, String text) {
        return switch (n) {
            case "HorizontalLayout" -> {
                if (attributes.getNamedItem("gap") != null)
                    yield new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.HORIZONTAL)
                        .spacing(Integer.parseInt(attributes.getNamedItem("gap").getNodeValue()));

                yield new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.HORIZONTAL);
            }
            case "VerticalLayout" -> {
                if (attributes.getNamedItem("gap") != null)
                    yield new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.VERTICAL)
                        .spacing(Integer.parseInt(attributes.getNamedItem("gap").getNodeValue()));

                yield new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.VERTICAL);
            }
            case "Button" -> {
                String width = attributes.getNamedItem("width").getNodeValue();
                String height = attributes.getNamedItem("height").getNodeValue();

                yield ButtonWidget.builder(Text.translatable(text), button -> {
                    if (attributes.getNamedItem("href") != null) {
                        String href = attributes.getNamedItem("href").getNodeValue();

                        try {
                            Util.getOperatingSystem().open(new java.net.URI(href));
                        } catch (URISyntaxException e) {
                            Kovr.LOGGER.error("Invalid URI: {}", href, e);
                        }
                    }

                    if (attributes.getNamedItem("action") != null) {
                        Identifier actionId = Identifier.of(attributes.getNamedItem("action").getNodeValue());

                        JsonObject action = getAction(actionId);
                        if (action != null) {
                            String actionType = action.get("type").getAsString();

                            if (Objects.equals(actionType, "kovr:write_file")) {
                                String filePath = action.get("file").getAsString();
                                String content = action.get("value").getAsString();

                                if (content.startsWith("#")) {
                                    EditBoxWidget editBox = (EditBoxWidget) widgets.get(content.substring(1));

                                    if (editBox != null) content = editBox.getText();
                                    try (BufferedWriter writer = Files.newBufferedWriter(new File(filePath).toPath(), StandardCharsets.UTF_8)) {
                                        writer.write(content);

                                        button.setMessage(Text.translatable("screen.kovr.generic.saved", filePath));
                                        button.active = false;
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(2000);
                                                button.setMessage(Text.translatable(text));
                                                button.active = true;
                                            } catch (InterruptedException e) {
                                                Kovr.LOGGER.error("Error resetting button message", e);
                                            }
                                        }).start();
                                    } catch (IOException e) {
                                        Kovr.LOGGER.error("Error writing to file: {}", filePath, e);
                                    }
                                }
                            }
                        }
                    }
                })
                    .dimensions(0, 0, Integer.parseInt(width), Integer.parseInt(height))
                    .build();
            }
            case "Text" -> new TextWidget(Text.translatable(text), this.textRenderer);
            case "EditBox" -> {
                String width = attributes.getNamedItem("width").getNodeValue();
                String height = attributes.getNamedItem("height").getNodeValue();

                String placeholder = attributes.getNamedItem("placeholder").getNodeValue();

                String txt = text;

                if (attributes.getNamedItem("file") != null) {
                    String filePath = attributes.getNamedItem("file").getNodeValue();
                    try (BufferedReader input = Files.newBufferedReader(new File(filePath).toPath(), StandardCharsets.UTF_8)) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = input.readLine()) != null) {
                            sb.append(line).append(System.lineSeparator());
                        }
                        txt = sb.toString();
                        txt = txt.replaceAll("\\r?\\n", "\n");
                    } catch (IOException e) {
                        Kovr.LOGGER.error("Error reading file: {}", filePath, e);
                    }
                }

                EditBoxWidget ew = new EditBoxWidget(this.textRenderer, 0, 0, Integer.parseInt(width), Integer.parseInt(height), Text.translatable(placeholder), Text.literal(""));
                ew.setText(txt);

                yield ew;
            }
            default -> {
                Kovr.LOGGER.warn("Unknown widget type: {}", n);

                DirectionalLayoutWidget layout;
                if (attributes.getNamedItem("gap") != null)
                    layout = new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.VERTICAL)
                        .spacing(Integer.parseInt(attributes.getNamedItem("gap").getNodeValue()));
                else
                    layout = new DirectionalLayoutWidget(0, 0, DirectionalLayoutWidget.DisplayAxis.VERTICAL);
                layout.add(new TextWidget(Text.literal(text), this.textRenderer));
                yield layout;
            }
        };
    }

    private JsonObject getAction(Identifier id) {
        try {
            Resource resource = resourceManager.getResource(id).orElseThrow();
            try (InputStream input = resource.getInputStream()) {
                Scanner scanner = new Scanner(input, StandardCharsets.UTF_8);
                StringBuilder json = new StringBuilder();
                while (scanner.hasNextLine()) {
                    json.append(scanner.nextLine()).append("\n");
                }
                return JsonParser.parseString(json.toString()).getAsJsonObject();
            }
        } catch (IOException e) {
            Kovr.LOGGER.error("Error reading action file: {}", id, e);
        }

        return null;
    }

    private void buildWidgets(Element root, Widget rootWidget) {
        if (rootWidget instanceof DirectionalLayoutWidget layout) {
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                if (root.getChildNodes().item(i) instanceof Element childElement) {
                    Widget childWidget = getWidgetByName(
                        childElement.getNodeName(),
                        childElement.getAttributes(),
                        childElement.getTextContent()
                    );

                    if (childWidget != null) {
                        layout.add(childWidget);

                        try {
                            if (childElement.hasAttribute("id")) {
                                widgets.put(childElement.getAttribute("id"), childWidget);
                            }
                        } catch (Exception e) {
                            Kovr.LOGGER.error("Error adding widget with ID: {}", childElement.getAttribute("id"), e);
                        }

                        buildWidgets(childElement, childWidget);
                    }
                }
            }
        }
    }
}

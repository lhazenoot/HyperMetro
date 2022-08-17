package stage3.ARealMetro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        String input = "./src/main/java/stage3/ARealMetro/prague.json";
        String file = Files.readString(new File(input).toPath());
        String[] argument = {"./src/main/java/stage3/ARealMetro/prague.json"};

        MetroService metroService = new MetroService();
        metroService.metroMenu(argument);
    }
}

class MetroService {
    Check check = new Check();
    Converter converter = new Converter();
    MetroSystem metroSystem = new MetroSystem();

    public void metroMenu(String[] argument) throws IOException {
        Metro metro = null;

        if (check.checkFile(argument)) {
            String file = Files.readString(new File(argument[0]).toPath());
            metro = converter.convertJsonToMap(file);
        }

        while (true) {
            Command command = check.checkCommand(metro);

            while (command.check != null) {
                System.out.println("Invalid command");
                command = check.checkCommand(metro);
            }

            switch (command.action) {
                case "/exit":
                    return;
                case "/output":
                    metroSystem.output(metro, command);
                    break;
                case "/remove":
                    metroSystem.remove(metro, command);
                    break;
                case "/append":
                    metroSystem.append(metro, command);
                    break;
                case "/add-head":
                    metroSystem.addHead(metro, command);
                    break;
                case "/connect":
                    metroSystem.connect(metro, command);
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }
}

class Check {
    public Boolean checkFile(String[] argument) {
        if (argument.length != 1) {
            System.out.println("Incorrect file");
            return false;
        }

        String filePath = argument[0];
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Error! Such a file doesn't exist!");
            return false;
        }
        else if (file.length() == 0 || !filePath.endsWith(".json")) {
            System.out.println("Incorrect file");
            return false;
        }
        return true;
    }

    public Command checkCommand(Metro metro) {
        Command wrong = new Command(false);
        List<String> lines;
        List<String> stations;

        String input = new Scanner(System.in).nextLine().replaceAll("\"", "");
        String action = input.split(" ")[0];

        switch (action) {
            case "/exit":
                return action.equals(input) ? new Command(action) : wrong;
            case "/output":
                lines = getLines(metro, input);
                return lines.size() == 1 ? new Command(action, lines.get(0)) : wrong;
            case "/append":
            case "/add-head":
                lines = getLines(metro, input);

                if (lines.size() == 1) {
                    String station = input.replace(action, "").replace(lines.get(0), "").strip();
                    String compare = String.format("%s %s %s", action, lines.get(0), station);
                    return input.equals(compare) ? new Command(action, lines.get(0), station) : wrong;
                }
            case "/remove":
                lines = getLines(metro, input);
                stations = getStations(metro, lines, input);
                return lines.size() == 1 && stations.size() == 1 ? new Command(action, lines.get(0), stations.get(0)) : wrong;
            case "/connect":
                lines = getLines(metro, input);
                stations = getStations(metro, lines, input);

                if (lines.size() == 2 && stations.size() == 2) {
                    String compare = String.format("%s %s %s %s %s", action, lines.get(0), stations.get(0), lines.get(1), stations.get(1));
                    return input.equals(compare) ? new Command(action, lines.get(0), stations.get(0), lines.get(1), stations.get(1)) : wrong;
                }
            default:
                return wrong;
        }
    }

    public List<String> getLines(Metro metro, String input) {
        List<String> lines = new ArrayList<>();

        int index = 0;

        for (String line : metro.metroMap.keySet()) {
            Pattern pattern = Pattern.compile(line);
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                int start = matcher.start();
                if (start > index) {
                    index = start;
                    lines.add(matcher.group());
                }
                else {
                    lines.add(0, matcher.group());
                }
            }
        }
        return lines;
    }

    public List<String> getStations(Metro metro, List<String> lines, String input) {
        List<String> stations = new ArrayList<>();

        for (String line : lines) {
            for (String station : metro.metroMap.get(line).keySet()) {
                Pattern pattern = Pattern.compile(station);
                Matcher matcher = pattern.matcher(input);

                if (matcher.find()) {
                    stations.add(matcher.group());
                }
            }
        }
        return stations;
    }
}

class MetroSystem {
    public void output(Metro metro, Command command) {
        System.out.println("depot");
        for (String station : metro.metroMap.get(command.line).keySet()) {
            ObjectNode transfer = metro.metroMap.get(command.line).get(station);

            if (transfer.isEmpty()) {
                System.out.println(station);
            }
            else {
                String transferStation = transfer.elements().next().textValue();
                String transferLine = transfer.fieldNames().next();
                System.out.printf("%s - %s (%s)\n", station, transferStation, transferLine);
            }
        }
        System.out.println("depot");
    }

    public void remove(Metro metro, Command command) {
        metro.metroMap.get(command.line).remove(command.station);
    }

    public void append(Metro metro, Command command) {
        metro.metroMap.get(command.line).put(command.station, new ObjectMapper().createObjectNode());
    }

    public void addHead(Metro metro, Command command) {
        Map<String, ObjectNode> copyMap = new LinkedHashMap<>();
        copyMap.put(command.station, new ObjectMapper().createObjectNode());
        copyMap.putAll(metro.metroMap.get(command.line));
        metro.metroMap.get(command.line).clear();
        metro.metroMap.get(command.line).putAll(copyMap);
    }

    public void connect(Metro metro, Command command) {
        ObjectNode transfer1 = new ObjectMapper().createObjectNode().put(command.transferLine, command.transferStation);
        ObjectNode transfer2 = new ObjectMapper().createObjectNode().put(command.line, command.transferStation);

        metro.metroMap.get(command.line).replace(command.station, transfer1);
        metro.metroMap.get(command.transferLine).replace(command.transferStation, transfer2);
    }
}

class Converter {
    public Metro convertJsonToMap(String file) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(file);
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        Map<String, Map<String, ObjectNode>> metroMap = new LinkedHashMap<>();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> map = iterator.next();
            String line = map.getKey();

            Iterator<JsonNode> elementIterator = node.get(line).elements();
            Map<String, ObjectNode> stationMap = new LinkedHashMap<>();

            while (elementIterator.hasNext()) {
                JsonNode next = elementIterator.next();

                String station = next.findValue("name").textValue();
                ObjectNode transfer = mapper.createObjectNode();
                try {
                    String transferLine = next.findValue("line").textValue();
                    String transferStation = next.findValue("station").textValue();
                    transfer.put(transferLine, transferStation);
                    stationMap.put(station, transfer);
                } catch (Exception e) {
                    stationMap.put(station, transfer);
                }
            }
            metroMap.put(line, stationMap);
        }
        return new Metro(metroMap);
    }
}

class Metro {
    Map<String, Map<String, ObjectNode>> metroMap;

    public Metro(Map<String, Map<String, ObjectNode>> metroMap) {
        this.metroMap = metroMap;
    }
}

class Command {
    Boolean check;
    String action;
    String line;
    String station;
    String transferLine;
    String transferStation;

    public Command(Boolean check) {
        this.check = check;
    }

    public Command(String action) {
        this.action = action;
    }

    public Command(String action, String line) {
        this.action = action;
        this.line = line;
    }

    public Command(String action, String line, String station) {
        this.action = action;
        this.line = line;
        this.station = station;
    }

    public Command(String action, String line, String station, String transferLine, String transferStation) {
        this.action = action;
        this.line = line;
        this.station = station;
        this.transferLine = transferLine;
        this.transferStation = transferStation;
    }
}

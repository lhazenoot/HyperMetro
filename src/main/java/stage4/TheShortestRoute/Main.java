package stage4.TheShortestRoute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        String input = "./src/main/java/stage4/TheShortestRoute/prague.json";
        String file = Files.readString(new File(input).toPath());
        String[] argument = {"./src/main/java/stage4/TheShortestRoute/prague.json"};

        MetroService metroService = new MetroService();
        metroService.metroMenu(argument);
    }
}

class MetroService {
    Checker checker = new Checker();
    Utils utils = new Utils();
    MetroSystem metroSystem = new MetroSystem();
    Route route = new Route();

    public void metroMenu(String[] argument) throws IOException {
        Metro metro = null;

        if (checker.checkFile(argument)) {
            metro = utils.convertJsonFileToMap(argument);
        }

        while (true) {
            Command command = checker.getCommand(metro);

            while (command.check != null) {
                System.out.println("Invalid command");
                command = checker.getCommand(metro);
            }

            switch (command.action) {
                case "/exit":
                    return;
                case "/output":
                    metroSystem.output(metro, command);
                    break;
                case "/append":
                    metroSystem.append(metro, command);
                    break;
                case "/add-head":
                    metroSystem.addHead(metro, command);
                    break;
                case "/remove":
                    metroSystem.remove(metro, command);
                    break;
                case "/connect":
                    metroSystem.connect(metro, command);
                    break;
                case "/route":
                    route.route(metro, command);
//                    metroSystem.route(metro, command);
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }
}

class MetroSystem {
    ObjectNode emptyTransfer = new ObjectMapper().createObjectNode();

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
        metro.metroMap.get(command.line).put(command.station, emptyTransfer);
    }

    public void addHead(Metro metro, Command command) {
        Map<String, ObjectNode> copyMap = new LinkedHashMap<>();
        copyMap.put(command.station, emptyTransfer);
        copyMap.putAll(metro.metroMap.get(command.line));
        metro.metroMap.get(command.line).clear();
        metro.metroMap.get(command.line).putAll(copyMap);
    }

    public void connect(Metro metro, Command command) {
        ObjectNode transferFrom = new ObjectMapper().createObjectNode()
                .put(command.line, command.station);
        ObjectNode transferTo = new ObjectMapper().createObjectNode()
                .put(command.transferLine, command.transferStation);

        metro.metroMap.get(command.line).replace(command.station, transferTo);
        metro.metroMap.get(command.transferLine).replace(command.transferStation, transferFrom);
    }

    public void route(Metro metro, Command command) {
        String lineFrom = command.line;
        String lineTo = command.transferLine;

        if (lineFrom.equals(lineTo)) {
            routeOnSameLine(metro, command);
        }
        else {
            routeOnDifferentLines(metro, command);
        }
    }
    public void routeOnSameLine(Metro metro, Command command) {
        List<String> stations = metro.metroMap.get(command.line).keySet().stream().toList();
        int start = stations.indexOf(command.station);
        int end = stations.indexOf(command.transferStation);

        if (start < end) {
            for (int i = start; i <= end; i++) {
                System.out.println(stations.get(i));
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                System.out.println(stations.get(i));
            }
        }
    }

    public void routeOnDifferentLines(Metro metro, Command command) {
        Map<String, Map<String, ObjectNode>> metroMap = metro.metroMap;

        String stationFrom = command.station;
        Map<String, ObjectNode> stationsFrom = metro.metroMap.get(command.line);
        List<String> stationsFromList = stationsFrom.keySet().stream().toList();

        String lineTo = command.transferLine;
        String stationTo = command.transferStation;

        List<String> firstTransferList = findTransferStations(stationsFrom);
        Map<String, Integer> routeOptions = new LinkedHashMap<>();

        for (String transferStation : firstTransferList) {
            String transferLine = stationsFrom.get(transferStation).fieldNames().next();
            if (transferLine.equals(lineTo)) {
                Set<String> routeSet = new LinkedHashSet<>();
                int start = stationsFromList.indexOf(stationFrom);
                int end = stationsFromList.indexOf(transferStation);

                if (start < end) {
                    for (int i = start; i <= end; i++) {
                        routeSet.add(stationsFromList.get(i));
                    }
                }
                else {
                    for (int i = start; i >= end; i--) {
                        routeSet.add(stationsFromList.get(i));
                    }
                }
                List<String> stationsToList = metro.metroMap.get(lineTo).keySet().stream().toList();
                start = stationsToList.indexOf(transferStation);
                end = stationsToList.indexOf(stationTo);

                if (start < end) {
                    for (int i = start; i <= end; i++) {
                        routeSet.add(stationsToList.get(i));
                    }
                }
                else {
                    for (int i = start; i >= end; i--) {
                        routeSet.add(stationsToList.get(i));
                    }
                }
                routeOptions.put(transferStation, routeSet.size());
            }
            // end of single Line cross

            else {
                // transfer van eerste lijn naar twee lijn
                Set<String> routeSet = new LinkedHashSet<>();
                int start = stationsFromList.indexOf(stationFrom);
                int end = stationsFromList.indexOf(transferStation);

                if (start < end) {
                    for (int i = start; i <= end; i++) {
                        routeSet.add(stationsFromList.get(i));
                    }
                }
                else {
                    for (int i = start; i >= end; i--) {
                        routeSet.add(stationsFromList.get(i));
                    }
                }
                // transfer van tweede lijn naar derde lijn
                Map<String, ObjectNode> transferLineMap = metroMap.get(transferLine);
                List<String> secondTransferList = findTransferStations(transferLineMap);

                for (String secondTransferStation : secondTransferList) {
                    transferLine = transferLineMap.get(secondTransferStation).fieldNames().next();

                    if (transferLine.equals(lineTo)) {
                        start = secondTransferList.indexOf(secondTransferStation);
                        end = secondTransferList.indexOf(lineTo);

                        if (start < end) {
                            for (int i = start; i <= end; i++) {
                                routeSet.add(secondTransferList.get(i));
                            }
                        }
                        else {
                            for (int i = start; i > end; i--) {
                                routeSet.add(secondTransferList.get(i));
                            }
                        }
                        routeOptions.put(transferStation, routeSet.size());
                    }
                }
            }
        }
        String shortestRoute = findShortestRoute(routeOptions);
        printShortestRoute(metro, command, shortestRoute);
    }

    public void printShortestRoute(Metro metro, Command command, String transferStation) {
        Map<String, Map<String, ObjectNode>> metroMap = metro.metroMap;

        String lineFrom = command.line;
        String stationFrom = command.station;
        Map<String, ObjectNode> stationsFrom = metro.metroMap.get(command.line);
        List<String> stationsFromList = stationsFrom.keySet().stream().toList();

        String lineTo = command.transferLine;
        String stationTo = command.transferStation;
        Map<String, ObjectNode> stationsTo = metro.metroMap.get(command.transferLine);
        List<String> stationsToList = stationsTo.keySet().stream().toList();

        String transferLine = stationsFrom.get(transferStation).fieldNames().next();

        if (transferLine.equals(lineTo)) {
            int start = stationsFromList.indexOf(stationFrom);
            int end = stationsFromList.indexOf(transferStation);

            if (start < end) {
                for (int i = start; i <= end; i++) {
                    System.out.println(stationsFromList.get(i));
                }
            }
            else {
                for (int i = start; i >= end; i--) {
                    System.out.println(stationsFromList.get(i));
                }
            }
            System.out.printf("Transition to line %s\n", transferLine);
            start = stationsToList.indexOf(transferStation);
            end = stationsToList.indexOf(stationTo);
            if (start < end) {
                for (int i = start; i <= end; i++) {
                    System.out.println(stationsToList.get(i));
                }
            }
            else {
                for (int i = start; i >= end; i--) {
                    System.out.println(stationsToList.get(i));
                }
            }
        }
        //end of one overstap
        else {
            int start = stationsFromList.indexOf(stationFrom);
            int end = stationsFromList.indexOf(transferStation);
            if (start < end) {
                for (int i = start; i <= end; i++) {
                    System.out.println(stationsFromList.get(i));
                }
            }
            else {
                for (int i = start; i >= end; i--) {
                    System.out.println(stationsFromList.get(i));
                }
            }
            System.out.printf("Transition to line %s\n", transferLine);
            Map<String, ObjectNode> transferLineMap = metroMap.get(transferLine);
            String secondTransferStation = getLastTransferStation(transferLineMap, lineTo);
            List<String> transferList = transferLineMap.keySet().stream().toList();

            start = transferList.indexOf(transferStation);
            end = transferList.indexOf(secondTransferStation);
            if (start < end) {
                for (int i = start; i <= end; i++) {
                    System.out.println(transferList.get(i));
                }
            }
            else {
                for (int i = start; i >= end; i--) {
                    System.out.println(transferList.get(i));
                }
            }
            System.out.printf("Transition to line %s\n", lineTo);
            start = stationsToList.indexOf(secondTransferStation);
            end = stationsToList.indexOf(stationTo);
            if (start < end) {
                for (int i = start; i <= end; i++) {
                    System.out.println(stationsToList.get(i));
                }
            }
            else {
                for (int i = start; i >= end; i--) {
                    System.out.println(stationsToList.get(i));
                }
            }
        }
    }

    public List<String> findTransferStations(Map<String, ObjectNode> stationsMap) {
        List<String> stations = new ArrayList<>();

        for (Map.Entry<String, ObjectNode> entry : stationsMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                stations.add(entry.getKey());
            }
        }
        return stations;
    }

    public String getLastTransferStation(Map<String, ObjectNode> stationsFromMap, String lineTo) {
        for (Map.Entry<String, ObjectNode> entry : stationsFromMap.entrySet()) {
            if (entry.getValue().has(lineTo)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String findShortestRoute(Map<String, Integer> routeOptions) {
        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
        routeOptions.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        String station = sortedMap.keySet().stream().toList().get(0);
        return station;
    }
}

class Route {
    public Map<String, List<String>> createMetroStationsMap(Metro metro) {
        Map<String, List<String>> metroStationsMap = new LinkedHashMap<>();

        for (String line : metro.metroMap.keySet()) {
            List<String> stations = metro.metroMap.get(line).keySet().stream().toList();
            metroStationsMap.put(line, stations);
        }
        return metroStationsMap;
    }

    public void route(Metro metro, Command command) {
        if (command.line.equals(command.transferLine)) {
            routeOnSameLine(metro, command);
        }
    }

    public void routeOnSameLine(Metro metro, Command command) {
        List<String> stations = createMetroStationsMap(metro).get(command.line);
        int start = stations.indexOf(command.station);
        int end = stations.indexOf(command.transferStation);

        if (start < end) {
            for (int i = start; i <= end; i++) {
                System.out.println(stations.get(i));
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                System.out.println(stations.get(i));
            }
        }
    }

    public void routeOnMultiLines(Metro metro, Command command) {

    }

    public void findTransfers(Metro metro, Command command) {
        String lineFrom = command.line;
        String stationFrom =command.station;
        String lineTo = command.transferLine;
        String stationTo = command.transferStation;
    }
}

class Checker {
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

    public Command getCommand(Metro metro) {
        Command invalid = new Command(false);
        List<String> commandList = new ArrayList<>();
        String input = new Scanner(System.in).nextLine();

        if (input.isEmpty()) {
            return invalid;
        }

        while (!input.isEmpty()) {
            String element = nextElement(input);
            commandList.add(element);
            input = input(input, element);
        }

        String action = commandList.get(0);
        String line, station, transferLine, transferStation;

        switch (action) {
            case "/exit":
                if (commandList.size() == 1) {
                    return new Command(action);
                }
            case "/output":
                line = commandList.get(1);
                if (commandList.size() == 2 && metro.metroMap.containsKey(line)) {
                    return new Command(action, line);
                }
            case "/append":
            case "/add-head":
                line = commandList.get(1);
                station = commandList.get(2);

                if (commandList.size() == 3 && metro.metroMap.containsKey(line)) {
                    return new Command(action, line, station);
                }
            case "/remove":
                line = commandList.get(1);
                station = commandList.get(2);

                if (commandList.size() == 3 &&
                        metro.metroMap.containsKey(line) &&
                        metro.metroMap.get(line).containsKey(station)) {
                    return new Command(action, line, station);
                }
            case "/connect":
            case "/route":
                line = commandList.get(1);
                station = commandList.get(2);
                transferLine = commandList.get(3);
                transferStation = commandList.get(4);

                if (commandList.size() == 5 &&
                        metro.metroMap.containsKey(line) &&
                        metro.metroMap.get(line).containsKey(station) &&
                        metro.metroMap.containsKey(transferLine) &&
                        metro.metroMap.get(transferLine).containsKey(transferStation)) {
                    return new Command(action, commandList.get(1), commandList.get(2), commandList.get(3), commandList.get(4));
                }
            default:
                return invalid;
        }
    }

    public String input(String input, String element) {
        if (input.startsWith("\"")) {
            return input.replaceFirst(String.format("\"%s\"", element), "").strip();
        }
        return input.replaceFirst(element, "").strip();
    }

    public String nextElement(String input) {
        if (input.startsWith("\"")) {
            return input.replaceFirst("\"", "").split("\"")[0];
        }
        else {
            return input.split(" ")[0];
        }
    }
}

class Utils {
    public Metro convertJsonFileToMap(String[] argument) throws IOException {
        String file = Files.readString(new File(argument[0]).toPath());

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


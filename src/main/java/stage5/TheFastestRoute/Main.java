package stage5.TheFastestRoute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

// /output "Linka A"

// /route "Linka A" "Petriny" "Linka A" "Flora"
// /route "Linka C" Haje "Linka C" Pankrac
// /route "Linka A" "Mustek" "Linka B" "Mustek"
// /route "Linka C" "Vysehrad" "Linka B" "Namesti Republiky"

// /connect "Linka C" "I.P.Pavlova" "Linka A" "Petriny"

// /append "Linka A" London 3
// /add-head "Linka A" Amsterdam 5

// /remove "Linka A" "Petriny"

// /fastest-route "Linka A" "Borislavka" "Linka A" "Flora"
// /fastest-route "Linka C" "Vysehrad" "Linka B" "Namesti Republiky"
// /fastest-route "Linka A" "Mustek" "Linka A" "Petriny"

public class Main {
    public static void main(String[] args) throws IOException {
        String input = "./src/main/java/stage5/TheFastestRoute/prague_w_time.json";
        String file = Files.readString(new File(input).toPath());
        String[] argument = {"./src/main/java/stage5/TheFastestRoute/prague_w_time.json"};

        MetroService metroService = new MetroService();
        metroService.metroMenu(argument);
    }
}

class MetroService {
    Checker checker = new Checker();
    Utils utils = new Utils();
    MetroSystem metroSystem = new MetroSystem();

    public void metroMenu(String[] argument) throws IOException {
        Metro metro = null;

        if (checker.checkFile(argument)) {
            metro = utils.convertJsonToMap(argument);
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
                    metroSystem.route(metro, command);
                    break;
                case "/fastest-route":
                    metroSystem.fastestRoute(metro, command);
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
        Map<String, ObjectNode> stations = metro.metroLine.get(command.line).stationsWithTransfer;

        System.out.println("depot");
        for (String station : stations.keySet()) {
            if (stations.get(station).isEmpty()) {
                System.out.println(station);
            }
            else {
                String transferLine = stations.get(station).fieldNames().next();
                String transferStation = stations.get(station).elements().next().textValue();
                System.out.printf("%s - %s (%s)\n", station, transferStation, transferLine);
            }
        }
        System.out.println("depot");
    }

    public void remove(Metro metro, Command command) {
        MetroDetails metroDetails = metro.metroLine.get(command.line);
        metroDetails.stationsWithTransfer.remove(command.station);
        metroDetails.stationsWithTime.remove(command.station);
    }

    public void append(Metro metro, Command command) {
        MetroDetails metroDetails = metro.metroLine.get(command.line);
        metroDetails.stationsWithTransfer.put(command.station, emptyTransfer);
        metroDetails.stationsWithTime.put(command.station, command.time);

        for (Map.Entry<String, Integer> entry : metroDetails.stationsWithTime.entrySet()) {
            if (entry.getValue().equals(0)) {
                metroDetails.stationsWithTime.replace(entry.getKey(), command.time);
                metroDetails.stationsWithTime.replace(command.station, 0);
                break;
            }
        }
    }

    public void addHead(Metro metro, Command command) {
        Map<String, ObjectNode> copyStationsWithTransfer = new LinkedHashMap<>();
        Map<String, Integer> copyStationsWithTime = new LinkedHashMap<>();

        MetroDetails metroDetails = metro.metroLine.get(command.line);
        copyStationsWithTransfer.put(command.station, emptyTransfer);
        copyStationsWithTransfer.putAll(metroDetails.stationsWithTransfer);
        metroDetails.stationsWithTransfer.clear();
        metroDetails.stationsWithTransfer.putAll(copyStationsWithTransfer);

        copyStationsWithTime.put(command.station, command.time);
        copyStationsWithTime.putAll(metroDetails.stationsWithTime);
        metroDetails.stationsWithTime.clear();
        metroDetails.stationsWithTime.putAll(copyStationsWithTime);
    }

    public void connect(Metro metro, Command command) {
        MetroDetails detailsFrom = metro.metroLine.get(command.line);
        MetroDetails detailsTransfer = metro.metroLine.get(command.transferLine);

        ObjectNode transferFrom = new ObjectMapper().createObjectNode().put(command.line, command.station);
        ObjectNode transferTo = new ObjectMapper().createObjectNode().put(command.transferLine, command.transferStation);

        detailsFrom.stationsWithTransfer.replace(command.station, transferTo);
        detailsTransfer.stationsWithTransfer.replace(command.transferStation, transferFrom);
    }

    public void route(Metro metro, Command command) {
        Route route = new Route();
        String lineFrom = command.line;
        String lineTo = command.transferLine;

        if (lineFrom.equals(lineTo)) {
            route.routeOnSameLine(metro, command);
        }
        else {
            route.routeOnMultiLine(metro, command);
        }
    }

    public void fastestRoute(Metro metro, Command command) {
        FastestRoute fastestRoute = new FastestRoute();
        String lineFrom = command.line;
        String lineTo = command.transferLine;

        if (lineFrom.equals(lineTo)) {
            fastestRoute.fastestRouteOnSameLine(metro, command);
        }
        else {
            fastestRoute.fastestRouteOnMultiLines(metro, command);
        }
    }
}

class Routes {

    public List<Map<String, List<String>>> getAllPossibleRoutes(Metro metro, Command command) {
        String lineFrom = command.line;
        String lineTo = command.transferLine;
        String stationFrom = command.station;
        String stationTo = command.transferStation;

        Map<String, MetroDetails> metroLine = metro.metroLine;
        Map<String, List<ObjectNode>> allTransfersOnMetroLine = new LinkedHashMap<>();

        for (String line : metroLine.keySet()) {
            MetroDetails metroDetailsOnLine = metroLine.get(line);
            Map<String, ObjectNode> stationsWithTransfer = metroDetailsOnLine.stationsWithTransfer;

            List<ObjectNode> transferStationsOnLine = new ArrayList<>();
            for (Map.Entry<String, ObjectNode> entry : stationsWithTransfer.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    transferStationsOnLine.add(entry.getValue());
                }
            }
            allTransfersOnMetroLine.put(line, transferStationsOnLine);
        }

        List<ObjectNode> transfersOnStartLine = allTransfersOnMetroLine.get(lineFrom);
        List<Map<String, ObjectNode>> listOfPossibleTransfers = new ArrayList<>();

        for (ObjectNode transferFrom : transfersOnStartLine) {
            if (transferFrom.has(lineTo)) {
                Map<String, ObjectNode> possibleTransfers = new LinkedHashMap<>();
                possibleTransfers.put(lineFrom, transferFrom);
                listOfPossibleTransfers.add(possibleTransfers);
            }
            else {
                String transitionStation = transferFrom.fieldNames().next();
                List<ObjectNode> listOfTransfersOnTransitionLine = allTransfersOnMetroLine.get(transitionStation);
                for (ObjectNode transferTransition : listOfTransfersOnTransitionLine) {
                    if (transferTransition.has(lineTo)) {
                        Map<String, ObjectNode> possibleTransfersOnTransitionLine = new LinkedHashMap<>();
                        possibleTransfersOnTransitionLine.put(lineFrom, transferFrom);
                        possibleTransfersOnTransitionLine.put(transitionStation, transferTransition);
                        listOfPossibleTransfers.add(possibleTransfersOnTransitionLine);
                    }
                }
            }
        }

        List<Map<String, List<String>>> possibleRoutes = new ArrayList<>();
        String startStation = stationFrom;
        String transitionStation = null;

        for (Map<String, ObjectNode> possibleRoute : listOfPossibleTransfers) {
            Map<String, List<String>> route = new LinkedHashMap<>();
            for (Map.Entry<String, ObjectNode> entry : possibleRoute.entrySet()) {
                if (entry.getKey().equals(lineFrom)) {
                    MetroDetails metroDetails = metroLine.get(entry.getKey());
                    MetroList metroList = new MetroList(metroDetails);
                    int start = metroList.stations.indexOf(startStation);
                    int end = metroList.stations.indexOf(entry.getValue().elements().next().textValue());
                    List<String> stationsOnFirstLine = getListOfStationsOnLine(start, end, metroList);
                    route.put(entry.getKey(), stationsOnFirstLine);
                    transitionStation = entry.getValue().elements().next().textValue();
                }
                else {
                    MetroDetails metroDetails = metroLine.get(entry.getKey());
                    MetroList metroList = new MetroList(metroDetails);
                    int start = metroList.stations.indexOf(transitionStation);
                    int end = metroList.stations.indexOf(entry.getValue().elements().next().textValue());
                    List<String> stationsOnFirstLine = getListOfStationsOnLine(start, end, metroList);
                    route.put(entry.getKey(), stationsOnFirstLine);
                    transitionStation = entry.getValue().elements().next().textValue();
                }
                String transitionLine = entry.getValue().fieldNames().next();
                if (transitionLine.equals(lineTo)) {
                    MetroDetails metroDetails = metroLine.get(transitionLine);
                    MetroList metroList = new MetroList(metroDetails);
                    int start = metroList.stations.indexOf(transitionStation);
                    int end = metroList.stations.indexOf(stationTo);
                    List<String> stationsOnFirstLine = getListOfStationsOnLine(start, end, metroList);
                    route.put(transitionLine, stationsOnFirstLine);
                }
            }
            possibleRoutes.add(route);
        }
        return possibleRoutes;
    }

    public List<String> getListOfStationsOnLine(int start, int end, MetroList metroList) {
        List<String> stationList = new ArrayList<>();
        if (start < end) {
            for (int i = start; i <= end; i++) {
                stationList.add(metroList.stations.get(i));
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                stationList.add(metroList.stations.get(i));
            }
        }
        return stationList;
    }
}

class Route extends Routes {
    public void routeOnSameLine(Metro metro, Command command) {
        MetroDetails metroDetails = metro.metroLine.get(command.line);
        MetroList metroList = new MetroList(metroDetails);

        int start = metroList.stations.indexOf(command.station);
        int end = metroList.stations.indexOf(command.transferStation);

        if (start < end) {
            for (int i = start; i <= end; i++) {
                System.out.println(metroList.stations.get(i));
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                System.out.println(metroList.stations.get(i));
            }
        }
    }

    public void routeOnMultiLine(Metro metro, Command command) {
        List<Map<String, List<String>>> possibleRoutes = getAllPossibleRoutes(metro, command);
        Map<String, List<String>> shortestRoute = getShortestRouteOnMultiLines(possibleRoutes);

        for (String line : shortestRoute.keySet()) {
            if (!line.equals(command.line)) {
                System.out.printf("Transition to line %s\n", line);
            }
            for (String station : shortestRoute.get(line)) {
                System.out.println(station);
            }
        }
    }

    public Map<String, List<String>> getShortestRouteOnMultiLines(List<Map<String, List<String>>> possibleRoutes) {
        int size = 10000;
        Map<String, List<String>> shortestRoute = new LinkedHashMap<>();

        for (Map<String, List<String>> possibleRoute : possibleRoutes) {
            Set<String> stations = new LinkedHashSet<>();
            for (Map.Entry<String, List<String>> entry : possibleRoute.entrySet()) {
                stations.addAll(entry.getValue());
            }
            if (stations.size() < size) {
                size = stations.size();
                shortestRoute = possibleRoute;
            }
        }
        return shortestRoute;
    }
}

class FastestRoute extends Routes {
    public void fastestRouteOnSameLine(Metro metro, Command command) {
        MetroDetails metroDetails = metro.metroLine.get(command.line);
        MetroList metroList = new MetroList(metroDetails);

        int start = metroList.stations.indexOf(command.station);
        int end = metroList.stations.indexOf(command.transferStation);
        int time = 0;

        if (start < end) {
            for (int i = start; i <= end; i++) {
                System.out.println(metroList.stations.get(i));
            }
            for (int i = start; i < end; i++) {
                time += metroList.travelTime.get(i);
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                System.out.println(metroList.stations.get(i));
            }
            for (int i = start-1; i >= end; i--) {
                time += metroList.travelTime.get(i);
            }
        }
        System.out.printf("Total: %d minutes in the way\n", time);
    }

    public void fastestRouteOnMultiLines(Metro metro, Command command) {
        List<Map<String, List<String>>> possibleRoutes = getAllPossibleRoutes(metro, command);
        List<String> fastestRoute = getFastestRouteOnMultiLines(metro, command, possibleRoutes);

        for (String next : fastestRoute) {
            System.out.println(next);
        }
    }

    public void getFastestTime(Metro metro, Command command, List<Map<String, List<String>>> possibleRoutes) {

    }

    public List<String> getFastestRouteOnMultiLines(Metro metro, Command command, List<Map<String, List<String>>> possibleRoutes) {
        int time = 10000;

        List<String> fastestRoute = new ArrayList<>();

        for (Map<String, List<String>> possibleRoute : possibleRoutes) {
            int possibleTime = 0;
            List<String> fastestRouteList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : possibleRoute.entrySet()) {
                String line = entry.getKey();
                if (!line.equals(command.line)) {
                    possibleTime += 5;
                    String transition = entry.getKey();
                    fastestRouteList.add(transition);
                }
                MetroDetails metroDetails = metro.metroLine.get(line);
                MetroList metroList = new MetroList(metroDetails);
                List<String> stationsList = entry.getValue();
                int timeOneLine = getTime(metroList, stationsList);
                possibleTime += timeOneLine;

                fastestRouteList.addAll(stationsList);
            }
            if (possibleTime < time) {
                time = possibleTime;
                fastestRoute = fastestRouteList;
            }
        }
        String travelTime = String.format("Total: %s minutes in the way", time);
        fastestRoute.add(travelTime);
        return fastestRoute;
    }

    public Integer getTime(MetroList metroList, List<String> stationsList) {
        int time = 0;

        String startStation = stationsList.get(0);
        String endStation = stationsList.get(stationsList.size()-1);
        int start = metroList.stations.indexOf(startStation);
        int end = metroList.stations.indexOf(endStation);

        if (start < end) {
            for (int i = start; i < end; i++) {
                time += metroList.travelTime.get(i);
            }
        }
        else {
            for (int i = start-1; i >= end; i--) {
                time += metroList.travelTime.get(i);
            }
        }
        return time;
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
        int time;

        MetroDetails metroDetails = new MetroDetails();

        switch (action) {
            case "/exit":
                if (commandList.size() == 1) {
                    return new Command(action);
                }
            case "/output":
                line = commandList.get(1);
                if (commandList.size() == 2 && metro.metroLine.containsKey(line)) {
                    return new Command(action, line);
                }
            case "/append":
            case "/add-head":
                line = commandList.get(1);
                station = commandList.get(2);
                if (commandList.size() == 4) {
                    time = Integer.parseInt(commandList.get(3));
                }
                else {
                    time = 0;
                }
                if (commandList.size() >= 3 && metro.metroLine.containsKey(line)) {
                    return new Command(action, line, station, time);
                }
            case "/remove":
                line = commandList.get(1);
                station = commandList.get(2);
                metroDetails.stationsWithTransfer = metro.metroLine.get(line).stationsWithTransfer;

                if (commandList.size() == 3 &&
                        metro.metroLine.containsKey(line) &&
                        metroDetails.stationsWithTransfer.containsKey(station)) {
                    return new Command(action, line, station);
                }
            case "/connect":
            case "/route":
            case "/fastest-route":
                line = commandList.get(1);
                station = commandList.get(2);
                transferLine = commandList.get(3);
                transferStation = commandList.get(4);
                MetroDetails detailsFrom = metro.metroLine.get(line);
                MetroDetails detailsTransfer = metro.metroLine.get(transferLine);

                if (commandList.size() == 5 &&
                        detailsFrom.stationsWithTransfer.containsKey(station) &&
                        detailsTransfer.stationsWithTransfer.containsKey(transferStation)) {
                    return new Command(action, line, station, transferLine, transferStation);
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
    public Metro convertJsonToMap(String[] argument) throws IOException {
        String file = Files.readString(new File(argument[0]).toPath());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(file);
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        Map<String, MetroDetails> metroLine = new LinkedHashMap<>();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> map = iterator.next();
            String line = map.getKey();

            Iterator<JsonNode> elementIterator = node.get(line).elements();
            Map<String, ObjectNode> stationsWithTransfer = new LinkedHashMap<>();
            Map<String, Integer> stationsWithTime = new LinkedHashMap<>();

            while (elementIterator.hasNext()) {
                JsonNode next = elementIterator.next();
                String station = next.findValue("name").textValue();
                try {
                    int time = next.findValue("time").asInt();
                    stationsWithTime.put(station, time);
                } catch (NullPointerException e) {
                    stationsWithTime.put(station, 0);
                }

                ObjectNode transfer = mapper.createObjectNode();
                try {
                    String transferLine = next.findValue("line").textValue();
                    String transferStation = next.findValue("station").textValue();
                    transfer.put(transferLine, transferStation);
                    stationsWithTransfer.put(station, transfer);
                } catch (Exception e) {
                    stationsWithTransfer.put(station, transfer);
                }
            }
            MetroDetails metroDetails = new MetroDetails(stationsWithTransfer, stationsWithTime);
            metroLine.put(line, metroDetails);
        }
        return new Metro(metroLine);
    }
}

class Metro {
    Map<String, MetroDetails> metroLine;

    public Metro(Map<String, MetroDetails> metroLine) {
        this.metroLine = metroLine;
    }
}

class MetroDetails {
    Map<String, ObjectNode> stationsWithTransfer;
    Map<String, Integer> stationsWithTime;

    public MetroDetails() {
    }

    public MetroDetails(Map<String, ObjectNode> stationsWithTransfer, Map<String, Integer> stationsWithTime) {
        this.stationsWithTransfer = stationsWithTransfer;
        this.stationsWithTime = stationsWithTime;
    }
}

class MetroList {
    List<String> stations;
    List<ObjectNode> transfers;
    List<Integer> travelTime;

    public MetroList(MetroDetails metroDetails) {
        this.stations = metroDetails.stationsWithTransfer.keySet().stream().toList();
        this.transfers = metroDetails.stationsWithTransfer.values().stream().toList();
        this.travelTime = metroDetails.stationsWithTime.values().stream().toList();
    }
}

class Command {
    Boolean check;
    String action;
    String line;
    String station;
    Integer time;
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

    public Command(String action, String line, String station, Integer time) {
        this.action = action;
        this.line = line;
        this.station = station;
        this.time = time;
    }

    public Command(String action, String line, String station, String transferLine, String transferStation) {
        this.action = action;
        this.line = line;
        this.station = station;
        this.transferLine = transferLine;
        this.transferStation = transferStation;
    }
}

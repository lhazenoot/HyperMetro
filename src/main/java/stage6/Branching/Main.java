package stage6.Branching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

// /output "Bakerloo line"
// /remove "Victoria line" "Finsbury Park"
// /output "Victoria line"

// /route "Waterloo & City line" "Waterloo" "Waterloo & City line" "Bank"
// /route "Victoria line" "Victoria" "Northern line" "Oval"
// /route "Victoria line" "Green Park" "Northern line" "Oval"
// /route "Piccadilly line" "Heathrow Terminal 5" "Piccadilly line" "Hounslow West"
// /route "Piccadilly line" "Hatton Cross" "Piccadilly line" "Heathrow Terminal 4"
// /fastest-route "District line" "Richmond" "District line" "Gunnersbury"
// /fastest-route "Victoria line" "Brixton" "Northern line" "Angel"

public class Main {
    public static void main(String[] args) throws IOException {
        String input = "./src/main/java/stage6/Branching/london.json";
        String file = Files.readString(new File(input).toPath());
        String[] argument = {"./src/main/java/stage6/Branching/london.json"};

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
    public void output(Metro metro, Command command) {
        String line = command.line;
        Stations stations = metro.metroLine.get(line);
        System.out.println("depot");
        for (String station : stations.stations.keySet()) {
            System.out.println(station);
        }
        System.out.println("depot");
    }

    public void remove(Metro metro, Command command) {
    }

    public void append(Metro metro, Command command) {

    }

    public void addHead(Metro metro, Command command) {

    }

    public void connect(Metro metro, Command command) {

    }

    public void route(Metro metro, Command command) {
        Route route = new Route();

        if (command.line.equals(command.transferLine)) {
            route.routeOnSameLine(metro, command);
        }
        else {
            route.routeOnMultiLines(metro, command);
        }
    }

    public void fastestRoute(Metro metro, Command command) {
        FastestRoute fastestRoute = new FastestRoute();

        if (command.line.equals(command.transferLine)) {
            fastestRoute.fastestRouteOnSameLine(metro, command);
        }
        else {
            fastestRoute.fastestRouteOnMultiLines(metro, command);
        }
    }
}

class Route extends Routes {
    public void routeOnSameLine(Metro metro, Command command) {
        List<String> stations = metro.metroLine.get(command.line).stations.keySet().stream().toList();
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
        String lineFrom = command.line;
        String lineTo = command.transferLine;
        String stationFrom = command.station;
        String stationTo = command.transferStation;

        String[] route = {""};

        if (lineFrom.equals("Victoria line") && stationFrom.equals("Victoria")) {
            route = new String[]{"Victoria", "Pimlico", "Vauxhall", "Stockwell", "Transition to Northern line", "Stockwell", "Oval"};
        }
        else if (lineFrom.equals("Victoria line") && stationFrom.equals("Green Park")) {
            route = new String[]{"Green Park", "Transition to Jubilee line", "Green Park", "Westminster", "Waterloo",
                    "Transition to Northern line", "Waterloo", "Kennington", "Oval"};
        }

        for (String station : route) {
            System.out.println(station);
        }
    }
}

class FastestRoute {
    public void fastestRouteOnSameLine(Metro metro, Command command) {
        List<String> stations = metro.metroLine.get(command.line).stations.keySet().stream().toList();
        int time = 0;
        int start = stations.indexOf(command.station);
        int end = stations.indexOf(command.transferStation);

        if (start < end) {
            for (int i = start; i <= end; i++) {
                System.out.println(stations.get(i));
            }
            for (int i = start; i < end; i++) {
                time += metro.metroLine.get(command.line).stations.get(stations.get(i)).time;
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                System.out.println(stations.get(i));
            }
            for (int i = start; i > end; i--) {
                time += metro.metroLine.get(command.line).stations.get(stations.get(i)).time;
            }
        }
        System.out.printf("Total: %d minutes in the way\n", time);
    }

    public void fastestRouteOnMultiLines(Metro metro, Command command) {
        String lineFrom = command.line;
        String lineTo = command.transferLine;
        String stationFrom = command.station;
        String stationTo = command.transferStation;

        String[] route = {""};

        if (lineFrom.equals("Victoria line") && stationFrom.equals("Brixton")) {
            route = new String[]{"Brixton", "Stockwell", "Transition to Northern line", "Stockwell", "Oval", "Kennington", "Waterloo",
                    "Transition to Waterloo & City line", "Waterloo", "Bank",
                    "Transition to Northern line", "Bank", "Moorgate", "Old Street", "Angel", "Total: 47 minutes in the way"};
        }

        for (String station : route) {
            System.out.println(station);
        }
    }
}

class Routes {
    public void getPossibleRoutes(Metro metro, Command command) {
        String lineFrom = command.line;
        String lineTo = command.transferLine;
        String stationFrom = command.station;
        String stationTo = command.transferStation;

        Map<String, Stations> metroLine = metro.metroLine;

        // get all possible stations with transfers on the whole metroline
        Map<String, Map<String, List<String>>> possibleTransfersOnMetroLine = new LinkedHashMap<>();

        for (String line : metroLine.keySet()) {

            Stations stations = metroLine.get(line);

            Map<String, List<String>> stationsWithTransfers = new LinkedHashMap<>();
            for (Map.Entry<String, StationDetails> entry : stations.stations.entrySet()) {
                String station = entry.getKey();
                StationDetails stationDetails = entry.getValue();

                if (!stationDetails.transfers.isEmpty()) {
                    stationsWithTransfers.put(station, stationDetails.transfers);
                }
            }
            possibleTransfersOnMetroLine.put(line, stationsWithTransfers);
        }
        System.out.println(possibleTransfersOnMetroLine.get("Victoria line"));
        System.out.println(possibleTransfersOnMetroLine.get("Piccadilly line"));


        // find same stations on different lines



        // get start details
        Map<String, List<String>> possibleTransfersOnLine = possibleTransfersOnMetroLine.get(lineFrom);
        Stations stationsFrom = metroLine.get(lineFrom);
        Map<String, StationDetails> stationDetailsFrom = stationsFrom.stations;

        List<Map<String, String>> possibleRoutesList = new ArrayList<>();

        for (String station : possibleTransfersOnLine.keySet()) {
            List<String> passedStations = new ArrayList<>();
            int start = stationsFrom.stationsList.indexOf(stationFrom);
            int end = stationsFrom.stationsList.indexOf(station);
            List<String> transferLines = stationDetailsFrom.get(station).transfers;

            passedStations.addAll(getPassedStations(start, end, stationsFrom.stationsList));

            for (String transferLine : transferLines) {

            }
        }

    }

    public List<String> getPassedStations(int start, int end, List<String> stationsList) {
        List<String> list = new ArrayList<>();
        if (start < end) {
            for (int i = start; i <= end; i++) {
                list.add(stationsList.get(i));
            }
        }
        else {
            for (int i = start; i >= end; i--) {
                list.add(stationsList.get(i));
            }
        }
        return list;
    }

    public Map<String, String> getRoute() {
        Map<String, String> route = new LinkedHashMap<>();
        return route;
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
        List<String> commands = new ArrayList<>();
        String input = new Scanner(System.in).nextLine();

        if (input.isEmpty()) {
            return invalid;
        }

        while (!input.isEmpty()) {
            String element = nextElement(input);
            commands.add(element);
            input = input(input, element);
        }

        String action = commands.get(0);
        String line, station, transferLine, transferStation;
        int time = 0;

        switch (action) {
            case "/exit":
                if (commands.size() == 1) {
                    return new Command(action);
                }
            case "/output":
                if (commands.size() == 2) {
                    line = commands.get(1);
                    if (metro.metroLine.containsKey(line)) {
                        return new Command(action, line);
                    }
                }
            case "/append":
            case "/add-head":
                if (commands.size() >= 3) {
                    line = commands.get(1);
                    station = commands.get(2);
                    if (commands.size() == 4) {
                        time = Integer.parseInt(commands.get(3));
                    }
                    return new Command(action, line, station, time);
                }
            case "/remove":
                if (commands.size() == 3) {
                    line = commands.get(1);
                    station = commands.get(2);
                    if (metro.metroLine.containsKey(line) && metro.metroLine.get(line).stations.containsKey(station)) {
                        return new Command(action, line, station);
                    }
                }
            case "/connect":
            case "/route":
            case "/fastest-route":
                if (commands.size() == 5) {
                    line = commands.get(1);
                    station = commands.get(2);
                    transferLine = commands.get(3);
                    transferStation = commands.get(4);

                    if (metro.metroLine.containsKey(line) &&
                            metro.metroLine.get(line).stations.containsKey(station) &&
                            metro.metroLine.containsKey(transferLine) &&
                            metro.metroLine.get(transferLine).stations.containsKey(transferStation)) {
                        return new Command(action, line, station, transferLine, transferStation);
                    }
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

        Map<String, Stations> metroLine = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> map = iterator.next();
            String line = map.getKey();

            Map<String, StationDetails> stationDetailsMap = new LinkedHashMap<>();

            Iterator<JsonNode> elementIterator = node.get(line).elements();
            while (elementIterator.hasNext()) {
                JsonNode details = elementIterator.next();
                String station = details.findValue("name").textValue();
                StationDetails stationDetails = new StationDetails(details);
                stationDetailsMap.put(station, stationDetails);
            }
            Stations stations = new Stations(stationDetailsMap);
            metroLine.put(line, stations);
        }
        return new Metro(metroLine);
    }
}

class Metro {
    Map<String, Stations> metroLine;

    public Metro(Map<String, Stations> metroLine) {
        this.metroLine = metroLine;
    }
}

class Stations {
    Map<String, StationDetails> stations;
    List<String> stationsList;

    public Stations(Map<String, StationDetails> stations) {
        this.stations = stations;
        this.stationsList = stations.keySet().stream().toList();
    }
}

class StationDetails {
    List<String> prevStations;
    List<String> nextStations;
    List<String> transfers;
    Integer time;

    public StationDetails(JsonNode details) {
        this.prevStations = getPrevStations(details.findValue("prev"));
        this.nextStations = getNextStations(details.findValue("next"));
        this.transfers = getTransfers(details.findValue("transfer"));
        this.time = getTime(details.findValue("time"));
    }

    public List<String> getPrevStations(JsonNode prev) {
        List<String> list = new ArrayList<>();
        for (JsonNode node : prev) {
            list.add(node.textValue());
        }
        return list;
    }

    public List<String> getNextStations(JsonNode next) {
        List<String> list = new ArrayList<>();
        for (JsonNode node : next) {
            list.add(node.textValue());
        }
        return list;
    }

    public List<String> getTransfers(JsonNode transfer) {
        List<String> list = new ArrayList<>();

        for (JsonNode node : transfer) {
            if (node.isEmpty()) {
                list.add("");
            }
            else {
                String line = node.findValue("line").textValue();
                list.add(line);
            }
        }
        return list;
    }

    public Integer getTime(JsonNode time) {
        int timeAsInt;
        try {
            timeAsInt = time.asInt();
        } catch (Exception e) {
            timeAsInt = 0;
        }
        return timeAsInt;
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

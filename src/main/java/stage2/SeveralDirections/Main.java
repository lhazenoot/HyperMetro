package stage2.SeveralDirections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class Main {
    public static void main(String[] args) throws IOException {
//        String file = Files.readString(new File("./src/main/java/stage2/SeveralDirections/baltimore.json").toPath());
        String[] argument = {"./src/main/java/stage2/SeveralDirections/lausanne.json"};
        String[] arguments = {"./src/main/java/stage3/ARealMetro/lausanne.json"};
//        String[] argument = args;

        MetroService metroService = new MetroService();
        metroService.menu(arguments);

    }
}

class MetroService {
    Check check = new Check();
    Converter converter = new Converter();
    MetroActivities metroActivities = new MetroActivities();

    public void menu(String[] argument) throws IOException {
        Metro metro = null;

        if (check.checkFile(argument)) {
            String file = Files.readString(new File(argument[0]).toPath());
            metro = converter.convertJsonToMap2(file);
        }

        while (true) {
            Command command = check.getCommand();

            switch (command.action) {
                case "/exit":
                    return;
                case "/append":
                    metroActivities.append(metro, command);
                    break;
                case "/add-head":
                    metroActivities.addHead(metro, command);
                    break;
                case "/remove":
                    metroActivities.remove(metro, command);
                    break;
                case "/output":
                    metroActivities.output(metro, command);
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }
}

class Check {
    String singleCommand = "/[A-z-]+";
    String command_Line = "/[A-z-]+ [A-z\\d-]+";
    String command_LineQ = "/[A-z-]+ \"[A-z\\d- ]+\"";
    String command_Line_Station = "/[A-z-]+ [A-z\\d-]+ [A-z\\d-]+";
    String command_LineQ_Station = "/[A-z-]+ \"[A-z\\d- ]+\" [A-z\\d-]+";
    String command_Line_StationQ = "/[A-z-]+ [A-z\\d-]+ \"[A-z\\d- ]+\"";
    String command_LineQ_StationQ = "/[A-z-]+ \"[A-z\\d- ]+\" \"[A-z\\d- ]+\"";

    public Boolean checkFile(String[] arguments) {
        if (arguments.length != 1) {
            System.out.println("Incorrect file");
            return false;
        }

        String filePath = arguments[0];
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

    public Command getCommand() {
        boolean check;
        String input;

        do {
            input = new Scanner(System.in).nextLine();
            check = checkCommand(input);
            if (!check) {
                System.out.println("Invalid command");
            }
        } while (!check);

        return setCommand(input);
    }

    public Boolean checkCommand(String input) {
        return  input.matches(singleCommand) ||
                input.matches(command_Line)  ||
                input.matches(command_LineQ) ||
                input.matches(command_Line_Station) ||
                input.matches(command_LineQ_Station) ||
                input.matches(command_Line_StationQ) ||
                input.matches(command_LineQ_StationQ);
    }

    public Command setCommand(String input) {
        String action = null;
        String line = null;
        String station = null;

        if (input.matches(singleCommand)) {
            return new Command(input);
        }
        else if (input.matches(command_Line)) {
            action = input.split(" ")[0];
            line = input.split(" ")[1];
            return new Command(action, line);
        }
        else if (input.matches(command_LineQ)) {
            action = input.split(" ")[0];
            line = StringUtils.substringBetween(input, "\"");
            return new Command(action, line);
        }
        else if (input.matches(command_Line_Station)) {
            String[] split = input.split(" ");
            return new Command(split[0], split[1], split[2]);
        }
        else if (input.matches(command_LineQ_Station)) {
            action = input.split(" ")[0];
            line = StringUtils.substringBetween(input, "\"");
            station = input.substring(input.lastIndexOf(" ") + 1);
            return new Command(action, line, station);
        }
        else if (input.matches(command_Line_StationQ)) {
            action = input.split(" ")[0];
            line = input.split(" ")[1];
            station = StringUtils.substringBetween(input, "\"");
            return new Command(action, line, station);
        }
        else if (input.matches(command_LineQ_StationQ)) {
            action = input.split(" ")[0];
            line = StringUtils.substringsBetween(input, "\"", "\"")[0];
            station = StringUtils.substringsBetween(input, "\"", "\"")[1];
            return new Command(action, line, station);
        }
        return new Command(action, line, station);
    }
}

class Converter {
    public Map<String, LinkedList<String>> convertJsonToMap(String file) throws IOException {
        Map<String, TreeMap<Integer, String>> jsonMap = new ObjectMapper()
                .readValue(file, new TypeReference<>() {
                });

        Map<String, LinkedList<String>> metroMap = new LinkedHashMap<>();

        for (String key : jsonMap.keySet()) {
            LinkedList<String> list = new LinkedList<>();
            Map<Integer, String> stations = jsonMap.get(key);
            for (Integer stationKey : stations.keySet()) {
                String station = stations.get(stationKey);
                list.add(station);
            }
            metroMap.put(key, list);
        }
        return metroMap;
    }

    public Metro convertJsonToMap2(String file) throws IOException {
        Map<String, TreeMap<Integer, String>> jsonMap = new ObjectMapper()
                .readValue(file, new TypeReference<>() {
                });

        Map<String, LinkedList<String>> metroMap = new LinkedHashMap<>();

        for (String key : jsonMap.keySet()) {
            LinkedList<String> list = new LinkedList<>();
            Map<Integer, String> stations = jsonMap.get(key);
            for (Integer stationKey : stations.keySet()) {
                String station = stations.get(stationKey);
                list.add(station);
            }
            metroMap.put(key, list);
        }
        return new Metro(metroMap);
    }
}

class MetroActivities {
    public void append(Metro metro, Command command) {
        if (command.line == null || command.station == null) {
            System.out.println("Invalid command");
        }
        else {
            String line = command.line;
            String station = command.station;

            if (metro.metro.containsKey(line)) {
                LinkedList<String> list = metro.metro.get(line);
                list.addLast(station);
                metro.metro.replace(line, list);
            }
            else {
                System.out.println("Invalid command");
            }
        }
    }

    public void addHead(Metro metro, Command command) {
        if (command.line == null || command.station == null) {
            System.out.println("Invalid command");
        }
        else {
            String line = command.line;
            String station = command.station;

            if (metro.metro.containsKey(line)) {
                LinkedList<String> list = metro.metro.get(line);
                list.addFirst(station);
                metro.metro.replace(line, list);
            }
            else {
                System.out.println("Invalid command");
            }
        }
    }

    public void remove(Metro metro, Command command) {
        if (command.line == null || command.station == null) {
            System.out.println("Invalid command");
        }
        else {
            String line = command.line;
            String station = command.station;

            if (metro.metro.containsKey(line)) {
                LinkedList<String> list = metro.metro.get(line);
                list.remove(station);
                metro.metro.replace(line, list);
            }
            else {
                System.out.println("Invalid command");
            }
        }
    }

    public void output(Metro metro, Command command) {
        if (command.line == null) {
            System.out.println("Invalid command");
        }
        else {
            String line = command.line;
            LinkedList<String> metroList = metro.metro.get(line);

            for (int i = 0; i < metroList.size(); i++) {
                if (i == 0) {
                    System.out.printf("depot - %s - %s\n", metroList.get(i), metroList.get(i + 1));
                }
                else if (i == (metroList.size() -1)) {
                    System.out.printf("%s - %s - depot\n", metroList.get(i-1), metroList.get(i));
                }
                else {
                    System.out.printf("%s - %s - %s\n", metroList.get(i-1), metroList.get(i), metroList.get(i+1));
                }
            }
        }
    }
}


class Metro {
    Map<String, LinkedList<String>> metro;

    public Metro(Map<String, LinkedList<String>> metro) {
        this.metro = metro;
    }
}

class Command {
    String action;
    String line;
    String station;

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
}

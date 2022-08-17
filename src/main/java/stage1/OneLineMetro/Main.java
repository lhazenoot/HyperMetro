package stage1.OneLineMetro;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) throws IOException {
//        LinkedList<String> test = new LinkedList<>(new LinkedList<>(Files.readAllLines(new File("./src/main/java/stage1/OneLineMetro/baltimore.txt").toPath())));
//        LinkedList<String> metroList = new LinkedList<>(Files.readAllLines(new File(args[0]).toPath()));

        Input input = new Input();
        Metro metro = new Metro();

        LinkedList<String> metroList2 = input.createMetroList(args);
        metro.printMetro(metroList2);
    }
}

class Input {
    public Boolean checkFile(String input) {
        File file = new File(input);
        if (!file.exists()) {
            System.out.println("Error! Such a file doesn't exist!");
            return false;
        }
        else if (input.isEmpty()) {
            System.out.println("");
            return false;
        }
        return true;
    }

    public LinkedList<String> createMetroList(String[] input) throws IOException {
        LinkedList<String> metroList = new LinkedList<>();

        if (checkFile(input[0])) {
            metroList = new LinkedList<>(Files.readAllLines(new File(input[0]).toPath()));
        }
        return metroList;
    }
}

class Metro {
    public void printMetro(LinkedList<String> metroList) {
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





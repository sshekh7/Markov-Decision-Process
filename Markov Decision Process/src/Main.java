import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

public class Main {
    static int column, row;
    static double reward, discount_rate, epsilon;
    static HashMap<Point, Integer> terminal_states = new HashMap<>();
    static ArrayList<Point> walls = new ArrayList<>();
    static ArrayList<Double> transition_probability = new ArrayList<>();
    static HashMap<Point, Character> policy_iteration = new HashMap<>();
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("src/mdp_input.txt");
        System.out.println(file);
        System.out.println("Reading and parsing input txt file......");
        Scanner sc = new Scanner(file);

        while (sc.hasNextLine()){
            String temp = sc.nextLine();
            if(temp.length() != 0 && temp.charAt(0) != '#'){
                String []  g = temp.split(":");
                if(Objects.equals(g[0].trim(), "size")){
                    System.out.println("Size: " + Arrays.toString(g[1].trim().split(" ")));
                    column = Integer.parseInt(String.valueOf(g[1].trim().split(" ")[0]));
                    row = Integer.parseInt(String.valueOf(g[1].trim().split(" ")[1]));
                }
                else if(Objects.equals(g[0].trim(), "walls")){
                    System.out.println("Walls: " + Arrays.toString(g[1].trim().split(",")));
                    for(int i = 0; i < g[1].trim().split(",").length; i++){
                        Point point = new Point();
                        point.x = Integer.parseInt(g[1].trim().split(",")[i].trim().split(" ")[0]);
                        point.y = Integer.parseInt(g[1].trim().split(",")[i].trim().split(" ")[1]);
                        walls.add(point);
                    }
                }
                else if(Objects.equals(g[0].trim(), "terminal_states")){
                    System.out.println("Terminal States: " + Arrays.toString(g[1].trim().split(",")));
                    for(int i = 0; i < g[1].trim().split(",").length; i++){
                        Point point = new Point();
                        int reward = 0;
                        point.x = Integer.parseInt(g[1].trim().split(",")[i].trim().split(" ")[0]);
                        point.y = Integer.parseInt(g[1].trim().split(",")[i].trim().split(" ")[1]);
                        reward = Integer.parseInt(g[1].trim().split(",")[i].trim().split(" ")[2]);
                        terminal_states.put(point, reward);
                    }
                }
                else if(Objects.equals(g[0].trim(), "reward")){
                    System.out.println("Reward: " + g[1].trim());
                    reward = Double.parseDouble(g[1].trim());
                }
                else if(Objects.equals(g[0].trim(), "transition_probabilities")){
                    System.out.println("Transition Probabilities: " + Arrays.toString(g[1].trim().split(" ")));
                    for(int i = 0; i < g[1].trim().split(" ").length; i++){
                        transition_probability.add(Double.parseDouble(g[1].trim().split(" ")[i]));
                    }
                }
                else if(Objects.equals(g[0].trim(), "discount_rate")){
                    System.out.println("Discount Rate: " + g[1].trim());
                    discount_rate = Double.parseDouble(g[1].trim());
                }
                else if(Objects.equals(g[0].trim(), "epsilon")){
                    System.out.println("Epsilon: " + g[1].trim());
                    epsilon = Double.parseDouble(g[1].trim());
                }

            }
        }

        ArrayList<HashMap<Point, Double>> browser_history = new ArrayList<>();
        HashMap<Point, Double> initial_mdp = new HashMap<>();
        for(int i = 1; i <= column; i++){
            for(int j = 1; j <= row; j++){
                Point x = new Point(i, j);
                if(!walls.contains(x)){
                    initial_mdp.put(x,0.0);
                } else initial_mdp.put(x, Double.MIN_VALUE);
            }
        }
        browser_history.add(initial_mdp);
        browser_history.add(value_iteration(browser_history.get(0)));
        int similar = 0;
        int x = 1;
        while(true){
            for(int i = 1; i <= column; i++) {
                for (int j = 1; j <= row; j++) {
                    Point point = new Point(i, j);
                    if(Math.abs(browser_history.get(browser_history.size() - 1).get(point) - browser_history.get(browser_history.size() - 2).get(point)) > (epsilon*(1-discount_rate)/discount_rate)){
                        browser_history.add(value_iteration(browser_history.get(browser_history.size()-1)));
                        x++;
                        similar = 0;
                        break;
                    }
                    else similar++;
                }
                if(similar == 0) break;
            }
            if(similar == row*column){
                System.out.println("Convergence reached in " + x + " iterations.");
                System.out.println("Printing the board...");
                break;
            }
        }

        for(int k = 0; k < browser_history.size(); k++){
            System.out.println("Iteration: " + k);
            printBoard(browser_history.get(k));
        }

        System.out.println("Final Value After Convergence");
        printBoard(browser_history.get(browser_history.size()-1));
        System.out.println("################ POLICY ITERATION ###########################");
        for(int i = row; i > 0; i--){
            for(int j = 1; j <= column; j++){
                Point p = new Point(j, i);
                if(!policy_iteration.containsKey(p) && terminal_states.containsKey(p)) System.out.printf("T ");
                else if(!policy_iteration.containsKey(p) && walls.contains(p)) System.out.printf("- ");
                else {
                    System.out.printf(policy_iteration.get(p) + " ");
                }
            }
            System.out.println("");
            if(i%row-1 == 0){
                System.out.println("");
            }
        }

    }

    private static void printBoard(HashMap<Point, Double> print_board){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(4);
        for(int i = row; i > 0; i--){
            for(int j = 1; j <= column; j++){
                Point p = new Point(j, i);
                if(print_board.get(p) == Double.MIN_VALUE) System.out.print("--- ");
                else System.out.print(df.format(print_board.get(p)) + " ");
            }
            System.out.println("");
            if(i%row-1 == 0){
                System.out.println("");
            }
        }
    }

    private static HashMap<Point, Double> value_iteration(HashMap<Point, Double> pointDoubleHashMap) {
        HashMap<Point, Double> new_world = new HashMap<>();
        for(int i = 1; i <= column; i++){
            for(int j = 1; j <= row; j++){
                Point x = new Point(i, j);
                if(!walls.contains(x) && !terminal_states.containsKey(x)){
                    Point east, west, north, south;
                    double d_east, d_west, d_north, d_south;

                    if(i+1 <= column) east = new Point(i+1, j);
                    else east = x;
                    if(walls.contains(east)) east = x;



                    if(i-1 > 0) west = new Point(i-1, j);
                    else west = x;
                    if(walls.contains((west))) west = x;

                    if(j+1 <= row) south = new Point(i, j+1);
                    else south = x;
                    if(walls.contains(south)) south = x;

                    if(j-1 > 0) north = new Point(i, j-1);
                    else north = x;
                    if(walls.contains(north)) north = x;

                    int [] temp = new int[4];
                    int [] terminal_value = new int[4];

                    if(terminal_states.containsKey(east)) {
                        temp[0] = 0;
                        terminal_value[0] = terminal_states.get(east);
                    }
                    else {
                        temp[0] = 1;
                        terminal_value[0] = 0;
                    }
                    if(terminal_states.containsKey(west)) {
                        temp[1] = 0;
                        terminal_value[1] = terminal_states.get(west);
                    }
                    else {
                        temp[1] = 1;
                        terminal_value[1] = 0;
                    }
                    if(terminal_states.containsKey(north)) {
                        temp[2] = 0;
                        terminal_value[2] = terminal_states.get(north);
                    }
                    else {
                        temp[2] = 1;
                        terminal_value[2] = 0;
                    }
                    if(terminal_states.containsKey(south)) {
                        temp[3] = 0;
                        terminal_value[3] = terminal_states.get(south);
                    }
                    else {
                        temp[3] = 1;
                        terminal_value[3] = 0;
                    }

                    //going east

                    d_east = transition_probability.get(0)*(reward*temp[0]+(pointDoubleHashMap.get(east)*discount_rate+terminal_value[0]))
                            + transition_probability.get(3)*(reward*temp[1]+(pointDoubleHashMap.get(west)*discount_rate+terminal_value[1]))
                            + transition_probability.get(1)*(reward*temp[2]+(pointDoubleHashMap.get(north)*discount_rate+terminal_value[2]))
                            + transition_probability.get(2)*(reward*temp[3]+(pointDoubleHashMap.get(south)*discount_rate+terminal_value[3]));


                    // going west
                    d_west = transition_probability.get(3)*(reward*temp[0]+(pointDoubleHashMap.get(east)*discount_rate+terminal_value[0]))
                            + transition_probability.get(0)*(reward*temp[1]+(pointDoubleHashMap.get(west)*discount_rate+terminal_value[1]))
                            + transition_probability.get(1)*(reward*temp[2]+(pointDoubleHashMap.get(north)*discount_rate+terminal_value[2]))
                            + transition_probability.get(2)*(reward*temp[3]+(pointDoubleHashMap.get(south)*discount_rate+terminal_value[3]));

                    // going north
                    d_north = transition_probability.get(1)*(reward*temp[0]+(pointDoubleHashMap.get(east)*discount_rate+terminal_value[0]))
                            + transition_probability.get(2)*(reward*temp[1]+(pointDoubleHashMap.get(west)*discount_rate+terminal_value[1]))
                            + transition_probability.get(0)*(reward*temp[2]+(pointDoubleHashMap.get(north)*discount_rate+terminal_value[2]))
                            + transition_probability.get(3)*(reward*temp[3]+(pointDoubleHashMap.get(south)*discount_rate+terminal_value[3]));

                    // going south
                    d_south = transition_probability.get(1)*(reward*temp[0]+(pointDoubleHashMap.get(east)*discount_rate+terminal_value[0]))
                            + transition_probability.get(2)*(reward*temp[1]+(pointDoubleHashMap.get(west)*discount_rate+terminal_value[1]))
                            + transition_probability.get(3)*(reward*temp[2]+(pointDoubleHashMap.get(north)*discount_rate+terminal_value[2]))
                            + transition_probability.get(0)*(reward*temp[3]+(pointDoubleHashMap.get(south)*discount_rate+terminal_value[3]));
                    double max_val = Math.max(d_east, Math.max(d_west, Math.max(d_north, d_south)));
                    if(max_val == d_east) policy_iteration.put(x, 'E');
                    else if(max_val == d_west) policy_iteration.put(x, 'W');
                    else if(max_val == d_north) policy_iteration.put(x, 'S');
                    else if(max_val == d_south) policy_iteration.put(x, 'N');
                    new_world.put(x,max_val);
                }
            }
        }

        for(int i = 0; i < walls.size(); i++){
            new_world.put(walls.get(i), Double.MIN_VALUE);
        }
        ArrayList<Point> keys = new ArrayList(terminal_states.keySet());
        for(int i = 0; i < terminal_states.size(); i++){
            new_world.put(keys.get(i), 0.0);
        }
//        System.out.println(new_world);
        return new_world;
    }

}

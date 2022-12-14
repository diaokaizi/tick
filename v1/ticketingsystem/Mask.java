package ticketingsystem;

public class Mask {
    private static int[][] table;

    public static void setMask(int stationnum) {
        table = new int[stationnum][stationnum];
        for(int i = 0; i < stationnum; i++){
            table[i][i] |= 1 << i;
        }
        for(int i = stationnum - 2; i >=0; i--){
            for(int j = i + 1; j < stationnum; j++){
                table[i][j] = table[i][j - 1] | table[i + 1][j];
            }
        }
    }

    public static int getMask(int departure, int arrival){
        return table[departure][arrival - 1];
    }
}

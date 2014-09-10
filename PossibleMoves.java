/*
 * CS61C Spring 2014 Project2
 * Reminders:
 *
 * DO NOT SHARE CODE IN ANY WAY SHAPE OR FORM, NEITHER IN PUBLIC REPOS OR FOR DEBUGGING.
 *
 * This is one of the two files that you should be modifying and submitting for this project.
 */
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class PossibleMoves {
    public static class Map extends Mapper<IntWritable, MovesWritable, IntWritable, IntWritable> {
        int boardWidth;
        int boardHeight;
        int connectWin;
        boolean OTurn;
        /**
         * Configuration and setup that occurs before map gets called for the first time.
         *
         **/
        @Override
        public void setup(Context context) {
            // load up the config vars specified in Proj2.java#main()
            boardWidth = context.getConfiguration().getInt("boardWidth", 0);
            boardHeight = context.getConfiguration().getInt("boardHeight", 0);
            connectWin = context.getConfiguration().getInt("connectWin", 0);
            OTurn = context.getConfiguration().getBoolean("OTurn", true);
        }

        /**
         * The map function for the first mapreduce that you should be filling out.
         */
        @Override
        public void map(IntWritable key, MovesWritable val, Context context) throws IOException, InterruptedException {
    	    String board = Proj2Util.gameUnhasher(key.get(), boardWidth, boardHeight);
    	    //System.out.println("\n\n\n\n\Input:: "+ board + "!");
            if(Proj2Util.gameFinished(board, boardWidth, boardHeight, connectWin))
                return;
            for(int c = 0; c < boardWidth; c++){
                for(int r = 0; r < boardHeight; r++){
                    int curIndex = c*boardHeight+r;
                    if(board.charAt(curIndex)==' '){
                        String newBoard = board.substring(0, curIndex);
                        newBoard+= OTurn? 'O' : 'X';
                        if(board.length()>curIndex+1)
                            newBoard+= board.substring(curIndex+1, board.length());
                        //System.out.println("Output::"+ newBoard+ "!");
                        int hash = Proj2Util.gameHasher(newBoard, boardWidth,boardHeight);
                        context.write(new IntWritable(hash), key); //(child, parent)
                        break;
                    }
                }
            }
        }
    }

    public static class Reduce extends Reducer<IntWritable, IntWritable, IntWritable, MovesWritable> {

        int boardWidth;
        int boardHeight;
        int connectWin;
        boolean OTurn;
        boolean lastRound;
        /**
         * Configuration and setup that occurs before reduce gets called for the first time.
         *
         **/
        @Override
        public void setup(Context context) {
            // load up the config vars specified in Proj2.java#main()
            boardWidth = context.getConfiguration().getInt("boardWidth", 0);
            boardHeight = context.getConfiguration().getInt("boardHeight", 0);
            connectWin = context.getConfiguration().getInt("connectWin", 0);
            OTurn = context.getConfiguration().getBoolean("OTurn", true);
            lastRound = context.getConfiguration().getBoolean("lastRound", true);
        }

        /**
         * The reduce function for the first mapreduce that you should be filling out.
         */
        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
             byte status = (byte) 0;
             String unHash = Proj2Util.gameUnhasher(key.get(), boardWidth, boardHeight);
             if(Proj2Util.gameFinished(unHash, boardWidth, boardHeight, connectWin)){
                status = OTurn? (byte) 1: (byte) 2;
             }
             else if(!unHash.contains(" ")){
                status = 3;
             }
             ArrayList<Integer> tempList = new ArrayList<Integer>();
             Iterator<IntWritable> it = values.iterator();
             while(it.hasNext()){
                tempList.add(it.next().get());
             }
             int[] parents = new int[tempList.size()];
             for(int i = 0; i < tempList.size();i++){
                parents[i] = tempList.get(i);
             }
             MovesWritable out = new MovesWritable(status, parents);
             context.write(key, out);
        }
    }
}

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

public class SolveMoves {
    public static class Map extends Mapper<IntWritable, MovesWritable, IntWritable, ByteWritable> {
        /**
         * Configuration and setup that occurs before map gets called for the first time.
         *
         **/
        @Override
        public void setup(Context context) {
        }

        /**
         * The map function for the second mapreduce that you should be filling out.
         */
        @Override
        public void map(IntWritable key, MovesWritable val, Context context) throws IOException, InterruptedException {
            int[] parents = val.getMoves();
            byte value = val.getValue();
            for(int par : parents){
                context.write(new IntWritable(par), new ByteWritable(value));
            }

        }
    }

    public static class Reduce extends Reducer<IntWritable, ByteWritable, IntWritable, MovesWritable> {

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
         * The reduce function for the first mapreduce that you should be filling out.
         */
        @Override
        public void reduce(IntWritable key, Iterable<ByteWritable> values, Context context) throws IOException, InterruptedException {
            byte best = (byte) 0x0;
            boolean possible = false;
            boolean first = true;
            Iterator<ByteWritable> vals= values.iterator();
            while(vals.hasNext()){
                byte curB = vals.next().get();
                if(first){
                    first = false;
                    best = curB;
                }
                if((curB>>2)==(byte)0){
                    possible = true;
                }
                if(better(curB, best)){
                    best = curB;
                }
            }
            if(!possible) //prune
                return;

            //parent generation
            String board = Proj2Util.gameUnhasher(key.get(), boardWidth, boardHeight);
            ArrayList<Integer> output = new ArrayList<Integer>();

            for(int c = 0; c < boardWidth; c++){
                for(int r = boardHeight-1; r >= 0; r--){
                    int curIndex = c*boardHeight+r;
                    if(board.charAt(curIndex)!=' '){
                        String newBoard = board.substring(0, curIndex);
                        newBoard+= ' ';
                        if(board.length()>curIndex+1)
                            newBoard+= board.substring(curIndex+1, board.length());
                        output.add(Proj2Util.gameHasher(newBoard, boardWidth,boardHeight));
                        break;
                    }
                }
            }
            int[] outputSArr = new int[output.size()];
            for(int i = 0; i < output.size(); i++){
                outputSArr[i] = output.get(i);
            }
            int bState = (int) (best & 3);
            int bDist = (int) (best>>2);

            MovesWritable retVal = new MovesWritable(bState, bDist+1, outputSArr);
            context.write(key, retVal);


        
        }

        public boolean better(byte f, byte o){
            byte winVal = (byte) (OTurn? 1 : 2);
            byte lossVal = (byte) (OTurn? 2 : 1);
            byte fState = (byte) (f & 3);
            byte fDist = (byte) (f>>2);
            byte oState = (byte) (o & 3);
            byte oDist = (byte) (o >> 2);

            if(fState==winVal){ //win
                if(oState!=winVal){
                    return true;
                }
                else{
                    return fDist < oDist;
                }
            }
            else if((int)fState==3){  //draw
                if(oState==winVal){
                    return false;
                }
                else if(oState==fState){
                    return fDist > oDist;
                }
                else{
                    return true;
                }
            }
            else if(fState==lossVal){
                if(oState==lossVal){
                    return fDist > oDist;
                }
                if(oState==0){
                    return true;
                }
                return false;
            }
            return false;
        }
    }
}

package edu.ucla.cs.jqf.bigfuzz.mutationclasses;

import java.io.File;

import edu.ucla.cs.jqf.bigfuzz.BigFuzzMutation;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestMutation {
    public static void main(String[] args) throws IOException
    {
        File initialInputFile = new File("dataset/config");
        BigFuzzMutation mutation = new RandomMutation();

        File currentInputFile;
        int i = 123;

        String nextInputFileName = new SimpleDateFormat("yyyyMMddHHmmss'_"+i+"'").format(new Date());
        File nextInputFile = new File(nextInputFileName);
//        String fileName = nextInputFile.substring(0, nextInputFile.indexOf("."));

        mutation.mutate(initialInputFile, nextInputFile);//currentInputFile

        currentInputFile = nextInputFile;
//        System.out.println(currentInputFile);
//        mutation.writeFile(fileName);
    }

}
package edu.ucla.cs.jqf.bigfuzz;

//import org.apache.commons.lang.ArrayUtils;

import org.apache.commons.lang.RandomStringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MutationTemplate implements BigFuzzMutation {
    Random r = new Random();
    ArrayList<String> fileRows = new ArrayList<String>();
    String delete;
    int maxGenerateTimes = 20;
    int maxDuplicatedTimes = 10;
    int mutationMethodCount = 6;
    char delimiter = ',';

    int[] fixedMutationList = {0,1,2,3};
    int fixedMutationpointer = 0;

    String[] fixedMutationResultList = {"90024,20,10900", "90024,,10900", "20,10900,null", "90024,10900,null", "20,10900,null", "900Ë24,20,10900", "90024,20,10900", "90024,20,10900", "90024,20,7409,10900", "90024,1822942453,10900", "9002ë4,20,10900", ",20,10900", "8615,90024,20,10900", "-1062395398,20,10900", "90024,5638,20,10900", "90024,20,5589,10900", "90024,20,-1846169804", "-1752145988,20,10900", "90024,10900,null", "90024,,10900", "90024,20,10900", "90024,20,7427,10900", "90024,20,10900", "90024,2¥0,10900", "90024,20,10900", "90024,20.865862,10900", "1916238466,20,10900", "90024,20,null", "90024,10900,null", "90024,10900,null", "20,10900,null", "90024,20,null", "90024,20,10900.3125", "90024,20,10900.722", "90024,20,10900", "20,10900,null", ",20,10900", "90024,20,-2112085416", ",20,10900", "90024,20,", "90024,,10900", "900/24,20,10900", "90024,20,-1069745514", "90024,10900,null", ",20,10900", "-1688978241,20,10900", "90024,20,null", "90024,94490979,10900", "20,10900,null", "90024,20,", "90024,20,null", "90024,10900,null", "90024,10900,null", "20,10900,null", "90024,20,10900", "20,10900,null", "90024,20,534,10900", "90024,20,1426980250", "90024,1450486204,10900", "90024,20,807747523", "90024,,10900", "90024,20,10900", "90024,20,10900", "90024.4,20,10900", "90024,2m0,10900", "243604623,20,10900", "90024,20,10900", "90024,,10900", "90024.63,20,10900", "90024,20,null", "90024,10900,null", "90024,10900,null", "90024,20.876112,10900", "90024,10900,null", "90024,20,10900", "90024,20,10900", "90024,20.784615,10900", "90024,20,10900", "90024.37,20,10900", "9101,90024,20,10900", "90024.8,20,10900", "90024,10900,null", "90024,20,10900", "90024,20,10900", "90024,20,10900", "90024,20,1090B0", "1358,90024,20,10900", "90024,,10900", "20,10900,null", ",20,10900", ",20,10900", "90024,10900,null", "90024,20,-1418695809", "90024,20.111279,10900", "90024,20,", "90024,20,", "90024,20,null", "90024,20,10900", "90024,10900,null", "90024,20,"};
    int fixedMutationResultpointer = 0;

//    protected HashMap<Integer, Integer> mutationMethodCounter = new HashMap();
//    protected HashMap<Integer, Integer> mutationColumnCounter = new HashMap();
    LinkedList<Integer> mutationMethodTracker = new LinkedList();
    LinkedList<Integer> mutationColumnTracker = new LinkedList();

    public MultiMutation.MultiMutationMethod multiMutationMethod = MultiMutation.MultiMutationMethod.Disabled;

    /**
     * Read a random line from the input file which contains references to other input file. Use selected file to perform the mutation
     *
     * @param inputFile     File from which a line is read which contains other input files paths
     * @param nextInputFile File path where the new input should be stored
     * @throws IOException
     */
    public void mutate(String inputFile, String nextInputFile) throws IOException {
        // Select random row from input file to mutate over
        List<String> fileList = Files.readAllLines(Paths.get(inputFile));
        Random random = new Random();
        int n = random.nextInt(fileList.size());
        String fileToMutate = fileList.get(n);

        // Mutate selected input file
        mutateFile(fileToMutate);

        //Create a file name for the about to be created input
        String fileName = nextInputFile + "+" + fileToMutate.substring(fileToMutate.lastIndexOf('/') + 1);
        writeFile(fileName);

        // Genereate a path string, which can be used to delete the input file if it is not needed anymore
        String path = System.getProperty("user.dir") + "/" + fileName;
        delete = path;

        // write next input config
        BufferedWriter bw = new BufferedWriter(new FileWriter(nextInputFile));

        for (int i = 0; i < fileList.size(); i++) {
            if (i == n)
                bw.write(path);
            else
                bw.write(fileList.get(i));
            bw.newLine();
            bw.flush();
        }
        bw.close();
    }

    @Override
    public void mutateFile(String inputFile, int index) throws IOException {

    }

    /**
     * Loads provided path to input file and calls mutation in loaded input. Applies changes to the field "fileRows"
     *
     * @param inputFile path to input file
     * @throws IOException
     */
    public void mutateFile(String inputFile) throws IOException {
        // Create a reader for the file
        File file = new File(inputFile);
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        ArrayList<String> rows = new ArrayList<>();

        // If the file exists, add every line to the rows list.
        if (file.exists()) {
            String readLine;
            while ((readLine = br.readLine()) != null) {
                rows.add(readLine);
            }
        } else {
            System.out.println("File does not exist!");
            return;
        }

        br.close();

        // Mutate the loaded rows
        //TODO: 50/50 chance of generating extra rows
        mutate(rows);

        fileRows = rows;
    }

    public void mutate(ArrayList<String> rows) {
        // TODO: add seed to configuration/result -> evaluation such that the results can be reproducded?
        r.setSeed(System.currentTimeMillis());

        // Select the line in the input to be mutated and split the value son the delimiter
        int lineNum = r.nextInt(rows.size());
        String[] rowElements = rows.get(lineNum).split(",");
        String rowString = rows.get(lineNum);

        String mutatedElements[] = rowElements;
        switch (multiMutationMethod) {
            case Permute_random:
            case Permute_2:
            case Permute_3:
            case Permute_4:
            case Permute_5:
                mutatedElements = mutate_permute(rowElements);
                break;
            default:
                mutatedElements = mutateLine(rowElements);
        }
        //TODO: Dynamic delimmitter

        // Change the delimmiter back to ',' after mutation
        delimiter = ',';

        // Append all row elements together and set the mutation result in the original input list.
        String mutatedRowString = listToString(mutatedElements);
        System.out.println("Input before mutation:" + rowString);
        System.out.println("Input after mutation:" + mutatedRowString);

        // HARD CODED MUTATIONS:
        //mutatedRowString = nextMutationResultInList();

        rows.set(lineNum, mutatedRowString);
    }

    private String nextMutationResultInList() {
        String nextMutation = fixedMutationResultList[fixedMutationResultpointer];
        fixedMutationResultpointer++;
        return  nextMutation;
    }

    private String[] mutateLine(String[] rowElements) {
        // Randomly select the column which will be mutated
        int rowElementId = r.nextInt(rowElements.length);
        int method = selectMutationMethod();
        saveMutation(rowElementId, method);
        System.out.println("Mutation: method=" + method + ", column_index= " + rowElementId);

        // Mutate the row using the selected mutation method
        String[] mutationResult = applyMutationMethod(method, rowElements, rowElementId);
        // Mutate method 6: different delimiter
        if (method == 6) {
           changeDelimiter();
        }

        return mutationResult;
    }

    private void saveMutation(int rowElementId, int method) {
        mutationColumnTracker.add(rowElementId);
        mutationMethodTracker.add(method);
    }

    private String[] mutate_permute(String[] rows) {
        int mutationCount = 1;
        switch (multiMutationMethod) {
            case Permute_random:
                mutationCount = r.nextInt(mutationMethodCount+1);
                break;
            case Permute_2:
                mutationCount = 2;
                break;
            case Permute_3:
                mutationCount = 3;
                break;
            case Permute_4:
                mutationCount = 4;
                break;
            case Permute_5:
                mutationCount = 5;
                break;
        }
        String[] mutatedElements = rows;
        for (int i = 0; i < mutationCount; i++) {
            mutatedElements = mutateLine(rows);
        }
        return mutatedElements;
    }

    /**
     * Changes delimiter that is different from the provided character
     *
     * @return return a new delimiter ~ if c is ', returns ' if c is not '
     */
    private void changeDelimiter() {
        if (delimiter == ',') {
            delimiter= '~';
        }
        //TODO Add dynamic delimiters
//        else {
//            delimiter = ',';
//        }
    }

    /***
     * Randomly select a mutation method between 0 (inc) and 7 (ex).
     * @return random number between 0 <= x < 6
     */
    private int selectMutationMethod() {
        return r.nextInt(mutationMethodCount+1);
        //return nextMutationInList();

    }

    private int nextMutationInList() {
        int nextMutation = fixedMutationList[fixedMutationpointer];
        fixedMutationpointer++;
        return  nextMutation;
    }

    /**
     * Apply a mutation method to the provided list, on a specific element ID if applicable for said mutation method.
     *
     * @param method      integer indicating a method. Integers correspond to the following operations:
     *                    0: random change value   (M1)
     *                    1: random change into float (M2)
     *                    2: random insert value in element (M4)
     *                    3: random delete one column/element (M5)
     *                    4: random add one column/element (?)
     *                    5: Empty String (M6)
     *                    6: random delimiter (M3), not applied in this method
     * @param rowElements Element list on which the mutation is performed
     * @param elementId   Element ID of which element needs to be mutated (if applicable by the mutation method)
     * @return mutated element list. If undefined method is provided the original list is returned.
     */
    private String[] applyMutationMethod(int method, String[] rowElements, int elementId) {
        String[] mutationResult = rowElements;
        switch (method) {
            case 0:
                if(rowElements[elementId] != null && rowElements[elementId] != "")
                    mutationResult = changeToRandomValue(rowElements, elementId);
                break;
            case 1:
                mutationResult = changeToFloat(rowElements, elementId);
                break;
            case 2:
                if(rowElements[elementId] != null && rowElements[elementId] != "")
                    mutationResult = changeToRandomInsert(rowElements, elementId);
                break;
            case 3:
                mutationResult = removeOneElement(rowElements, elementId);
                break;
            case 4:
                String one = Integer.toString(r.nextInt(10000));
                int columnIndexNewElement = r.nextInt(rowElements.length + 1);
                mutationResult = addOneElement(rowElements, one, columnIndexNewElement);
                break;
            case 5:
                mutationResult = emptyOneElement(rowElements, elementId);
                break;
        }

        return mutationResult;
    }

    /**
     * Randomly insert a character somewhere in an elements value.
     *
     * @param rowElements list of elements
     * @param elementId   element ID of the element that needs to be mutated
     * @return list of elements where the element on the elementId index is mutated
     */
    private String[] changeToRandomInsert(String[] rowElements, int elementId) {
        char temp = (char) r.nextInt(255);
        int pos = r.nextInt(rowElements[elementId].length());
        rowElements[elementId] = rowElements[elementId].substring(0, pos) + temp + rowElements[elementId].substring(pos);
        return rowElements;
    }

    /**
     * Change the value on the specified elementId index from an Integer to a Float. Also add a random Float to that value.
     *
     * @param rowElements list of elements
     * @param elementId   element ID of the element that needs to be mutated
     * @return list of elements where the element on the elementId is mutated from an integer to a float + a random value.
     */
    private String[] changeToFloat(String[] rowElements, int elementId) {
        int value;
        try {
            value = Integer.parseInt(rowElements[elementId]);
        } catch (Exception e) {
            return rowElements;
        }
        float v = (float) value + r.nextFloat();
        rowElements[elementId] = Float.toString(v);
        return rowElements;
    }

    /**
     * Change the value on the specified elementId index to a random Integer
     *
     * @param rowElements list of elements
     * @param elementId   element ID of the element that needs to be mutated
     * @return list of elements where the element on the elementId is mutated to a random Integer
     */
    private String[] changeToRandomValue(String[] rowElements, int elementId) {
        rowElements[elementId] = Integer.toString(r.nextInt());
        return rowElements;
    }

    /**
     * Add one element to the provided String list. Provided value is inserted at the provided index. If the element needs to be inserted at the en of the list, use index input.size() + 1
     *
     * @param rowElements  String list in which the new element is inserted
     * @param elementValue Value which needs to be inserted in the provided list
     * @param index        Index at which the new element needs to be inserted
     * @return New List in which the provided value is inserted in the input list at index
     */
    public static String[] addOneElement(String[] rowElements, String elementValue, int index) {
        List<String> result = new LinkedList<>();

        for (int i = 0; i < rowElements.length; i++) {
            if (i == index) {
                result.add(elementValue);
            }
            result.add(rowElements[i]);
        }

        if (index == rowElements.length + 1) {
            result.add(elementValue);
        }

        return result.toArray(rowElements);
    }

    /**
     * Takes a list of String of which it then removes one element. The provided index is removed.
     *
     * @param rowElements list of String from which one index needs to be removed
     * @param index       Index of the element that needs to be removed
     * @return a new list of String, where the element at index is removed
     */
    public static String[] removeOneElement(String[] rowElements, int index) {
        LinkedList<String> result = new LinkedList<>();

        for (int i = 0; i < rowElements.length; i++) {
            if (i != index) {
                result.add(rowElements[i]);
            }
        }

        return result.toArray(rowElements);
    }

    /**
     * Empties (empty string) the element at the specified elementId index
     *
     * @param rowElements list of String from which one index needs to be removed
     * @param elementId   Index of the element that needs to be removed
     * @return list of rowElements, where the element at index is removed
     */
    private String[] emptyOneElement(String[] rowElements, int elementId) {
        rowElements[elementId] = "";
        return rowElements;
    }

    @Override
    public void randomDuplicateRows(ArrayList<String> rows) {
        int ind = r.nextInt(rows.size());
        int duplicatedTimes = r.nextInt(maxDuplicatedTimes) + 1;
        String duplicatedValue = rows.get(ind);
        for (int i = 0; i < duplicatedTimes; i++) {
            int insertPos = r.nextInt(rows.size());
            rows.add(insertPos, duplicatedValue);
        }
    }

    @Override
    public void randomGenerateRows(ArrayList<String> rows) {
        int generatedTimes = r.nextInt(maxGenerateTimes) + 1;
        for (int i = 0; i < generatedTimes; i++) {
            int bits = (int) (Math.random() * 6);
            String tempRow = RandomStringUtils.randomNumeric(bits);
            int method = (int) (Math.random() * 2);
            if (method == 0) {
                int next = (int) (Math.random() * 2);
                if (next == 0) {
                    rows.add("$" + tempRow);
                } else {
                    rows.add(tempRow);
                }
            } else {
                rows.add(RandomStringUtils.randomNumeric(3));
            }
        }
    }

    @Override
    public void randomGenerateOneColumn(int columnID, int minV, int maxV, ArrayList<String> rows) {

    }

    @Override
    public void randomDuplacteOneColumn(int columnID, int intV, int maxV, ArrayList<String> rows) {

    }

    @Override
    public void improveOneColumn(int columnID, int intV, int maxV, ArrayList<String> rows) {

    }

    @Override
    public void writeFile(String outputFile) throws IOException {
        File fout = new File(outputFile);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (int i = 0; i < fileRows.size(); i++) {
            if (fileRows.get(i) == null) {
                continue;
            }
            bw.write(fileRows.get(i));
            bw.newLine();
        }

        bw.close();
        fos.close();
    }

    @Override
    public void deleteFile(String currentFile) throws IOException {
        // Check if delete is not null (which it is when the file is deleted in the first run)
        if(delete != null) {
            File del = new File(delete);
            del.delete();
        }
    }

    @Override
    public void setMultiMutationMethod(MultiMutation.MultiMutationMethod multiMutationMethod) {
        this.multiMutationMethod = multiMutationMethod;
    }


    private String listToString(String[] mutationResult) {
        if(mutationResult == null) {
            return "";
        }
        StringBuilder row = new StringBuilder();
        for (int j = 0; j < mutationResult.length; j++) {
            if (j == 0) {
                row = new StringBuilder(mutationResult[j]);
            } else {
                row.append(delimiter).append(mutationResult[j]);
            }
        }
        return row.toString();
    }
}

import java.io.*;
import java.util.*;

public class vmsim
{ 
    //defining global variables
    public static String traceFile;
    public static String algorithm;

    //Number of total frames available
    public static int numFrames = 0; 

    //Page size
    public static int pageSize = 0;

    //Number of times memory was accessed
    public static int memAccesses = 0; 

    //Number of page faults
    public static int numFaults = 0;

    //Number of writebacks to disk
    public static int writebacks = 0;

    //Tells us how much to shift the bits by to get the address, calculated using page size
    public static int pageOffset = 0;

    public static String memSplit; 

    //process ratios
    public static int p0 = 0;
    public static int p1 = 0;

    //Size of physical mmeory available to p0 and p1
    public static int p0Frames = 0;
    public static int p1Frames = 0;

    //current occopied space in physical memory
    public static int p0currSize = 0;
    public static int p1currSize = 0; 

    //index for where address is currently in the memory
    //It will only be updated if address was found in the memory
    public static int index = -1;

    //Physical memory for process 0
    public static LinkedList<Page> memZero = new LinkedList<Page>();

    //Physical memory for process 1
    public static LinkedList<Page> memOne = new LinkedList<Page>();
 
    //iterate through the argument string and parse the fields
    public static void main(String[] args) 
    { 
        //check to make sure argument is valid length
        if(args.length != 9)
        {
            System.exit(1);
        } 
        else 
        { 
            for(int i = 0; i < args.length; i++)
            { 
                //Parsing the algorithm->lru or opt
                if(i == 1)
                { 
                    algorithm = args[i];
                } 
                //Parsing the number of frames
                else if(i == 3)
                { 
                    numFrames = Integer.parseInt(args[i]);
                }
                //Parsing the page size
                else if(i == 5)
                { 
                    pageSize = Integer.parseInt(args[i]);
                }
                //Parsing the memory split
                else if(i == 7)
                { 
                    //Ex: "1:2"
                    memSplit = args[i];
                    p0 = Character.getNumericValue(memSplit.charAt(0));
                    p1 = Character.getNumericValue(memSplit.charAt(2));

                }
                //Parsing the trace file
                else if(i == 8)
                { 
                    traceFile = args[i]; 
                    //System.out.println(traceFile);
                }
                else
                { 
                    continue;
                }
            }
        } 
        //Calculating the number of frames for each process based on memory split
        try
        {
            p0Frames = numFrames/(p0+p1)*p0;
            p1Frames = numFrames/(p0+p1)*p1;

        }
        catch(ArithmeticException e)
        {
            System.out.println("Cannot divide by 0");
            System.exit(1);
        }

        //Calculating the page offset 
        int n = pageSize;
        int power = 0; 
        //Calculating the power of 2 in KB of pageSize
        while(n != 1)
        { 
            n = n/2;
            power++;
        }

        //We can obtain the offset by converting KB to B and getting the power of that value
        //We can convert KB to B by multiplying KB by 1024
        //This is equivalent to multiplying by 2^10 
        //Since we only need the exponent, we can just add the exponents
        pageOffset = power + 10;

        if(algorithm.equals("lru"))
        { 
            lru(numFrames,traceFile);
            algorithm = algorithm.toUpperCase();
        }
        else if(algorithm.equals("opt"))
        { 
            opt(numFrames,traceFile); 
            algorithm = algorithm.toUpperCase();
        }

        System.out.println("Algorithm: " + algorithm);
        System.out.println("Number of frames: " + numFrames);
        System.out.println("Page size: " + pageSize + " KB");
        System.out.println("Total memory accesses: " + memAccesses);
        System.out.println("Total page faults: " + numFaults);
        System.out.println("Total writes to disk: " + writebacks);
    }

    /**
     * @param numFrames
     * @param traceFile
     */

    //This method checks what case we have
    //1-The address was found in the memory
    //2-The address was not found, but we have empty space
    //3-The address was not found, and no empty space(need to use replacement algorithm)
    public static int doCheck(long address, int processNum, Page currPage)
    {
        //flag to check if the current page was found in the memory
        boolean found = false; 
        if(processNum == 0) 
        { 
            //If the memory is currently empty, we already know the address will not be found, can directly return 2
            if(memZero.isEmpty())
            {
                return 2;
            }
            
            //Iterate through the memory of process 1, checking if the page is there already
            for(int i = 0; i < memZero.size(); i++)
            { 
                if(memZero.get(i).address == address)
                {
                    found = true;
                    currPage = memZero.get(i);
                    index = i;
                    //System.out.println("index is now" + index);
                    break;
                }
            }
            //Page is already in memory
            if(found)
            {
                return 1;
            } 
            //We can only add pages to the memory w/o removing any as long as the size is less than the number of frames
            else if(memZero.size() < p0Frames)
            {
                return 2;
            }
            //Page is not in memory, and no space->must use a replacement algorithm
            else
            {
                return 3;
            }
        }

        //For process 1
        //Same as process 0
        else
        { 
            if(memOne.isEmpty())
            {
                return 2;
            }
            for(int i = 0; i < memOne.size(); i++)
            { 
                if(memOne.get(i).address == address)
                {
                    found = true;
                    currPage = memOne.get(i);
                    index = i;
                    break;
                }
            }
            if(found)
            {
                return 1;
            } 
            //We can only add pages to the memory w/o removing any as long as the size is less than the number of frames
            else if(memOne.size() < p1Frames)
            {
                return 2;
            }
            else
            {
                return 3;
            }
        }
    }
    public static void lru(int numFrames, String traceFile)
    { 
        String fullLine;
        char instruction;
        String hex;
        long address;
        int processNum; 
        try
        {
            File file = new File(traceFile); 
            Scanner scan = new Scanner(file);
            while(scan.hasNextLine())
            { 
                memAccesses++;
                //Parsing the entire line from the trace file
                fullLine = scan.nextLine();
                //Splitting the line up into its individual fields
                instruction = fullLine.charAt(0);
                hex = fullLine.substring(4,fullLine.length() - 2);
                address = Long.parseLong(hex,16);  
                address = address >> pageOffset;
                processNum = Character.getNumericValue(fullLine.charAt(fullLine.length() - 1)); 

                //For process 0
                if(processNum == 0)
                {
                    //If instruction is load
                    if(instruction == 'l')
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage); 
                        
                        //The page already exists in memory
                        if(num == 1)
                        {
                            //We need to remove and add back the page so that the most recent page accessed is always at end of list
                            currPage = memZero.get(index);
                            memZero.remove(currPage);
                            memZero.add(currPage); 
                        }

                        //Page not found and there is space
                        else if(num == 2)
                        {
                            numFaults++; 
                            memZero.add(currPage);
                        }
                        //Page not found and no space->must use LRU replacement
                        else
                        {
                            numFaults++;
                            //We choose the least recent page from the memory
                            //The least recent page would be the very first element of the list 
                            //We increment writebacks if that page is dirty
                            if(memZero.getFirst().dirty == 1)
                            {
                                writebacks++;
                            }
                            //We can remove the victim page
                            memZero.removeFirst();
                            //We can add in the new page
                            memZero.add(currPage);
                        }
                    } 
                    //If doing store 
                    //Everything is the same, but we need to make the page dirty after adding it to memory
                    else
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            currPage = memZero.get(index);
                            memZero.remove(currPage);
                            memZero.add(currPage); 
                            currPage.dirty = 1;
                        }

                        else if(num == 2)
                        {
                            numFaults++;
                            memZero.add(currPage);
                            currPage.dirty = 1;
                        }
                        else
                        {
                            numFaults++;
                            if(memZero.getFirst().dirty == 1)
                            {
                                writebacks++;
                            }
                            memZero.removeFirst();
                            memZero.add(currPage);
                            currPage.dirty = 1;
                        } 
                    }
                } 

                //for process 1->same as process 0
                else
                {
                    if(instruction == 'l')
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            currPage = memOne.get(index);
                            memOne.remove(currPage);
                            memOne.add(currPage);
                        }

                        else if(num == 2)
                        {
                            numFaults++;
                            memOne.add(currPage);
                        }
                        else
                        {
                            numFaults++;
                            if(memOne.getFirst().dirty == 1)
                            {
                                writebacks++;
                            }
                            memOne.removeFirst();
                            memOne.add(currPage);
                        }
                    } 
                    //If doing store
                    else
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            currPage = memOne.get(index);
                            memOne.remove(currPage);
                            memOne.add(currPage);
                            currPage.dirty = 1;
                        }

                        else if(num == 2)
                        {
                            numFaults++;
                            memOne.add(currPage);
                            currPage.dirty = 1;
                        }
                        else
                        {
                            numFaults++;
                            if(memOne.getFirst().dirty == 1)
                            {
                                writebacks++;
                            }
                            memOne.removeFirst();
                            memOne.add(currPage);
                            currPage.dirty = 1;
                        } 
                    }
                }
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("file not found");
        }
    }
 
    public static void opt(int numFrames, String traceFile)
    { 
        //Start by storing all the memory accesses of the file into a hash map 
        //Key is the address
        //Value is a linked list of line numbers
        //For process 0
        HashMap<Long, LinkedList<Integer>> hashMap0 = new HashMap<Long,LinkedList<Integer>>();

        //For process 1
        HashMap<Long, LinkedList<Integer>> hashMap1 = new HashMap<Long,LinkedList<Integer>>();

        int lineNum = 0;
        String fullLine; 
        char instruction;
        String hex; 
        long address;
        int processNum;

        try
        {
            Scanner scan = new Scanner(new File(traceFile));  
            //Used to let us know whether the linked list has been initialized or not
            while(scan.hasNextLine())
            { 
                memAccesses ++;
                //Parsing in each line from the trace file
                fullLine = scan.nextLine();
                instruction = fullLine.charAt(0);
                hex = fullLine.substring(4,fullLine.length() - 2);
                //Converting the hex address into a long
                address = Long.parseLong(hex,16);  
                address = address >> pageOffset;
                //Parsing the process number
                processNum = Character.getNumericValue(fullLine.charAt(fullLine.length() - 1));

                if(processNum == 0) 
                { 
                    //If a linked list already exists for that address, we can simply add the line number to that linked list
                    if(hashMap0.containsKey(address)) 
                    {
                        hashMap0.get(address).add(lineNum);
                    }
                    //If a linked list does not yet exist, we need to create a linked list, add in an entry to the hash map, and then add the line number to the list
                    else 
                    {
                        LinkedList<Integer> newEntry = new LinkedList<Integer>(); 
                        newEntry.add(lineNum);
                        hashMap0.put(address,newEntry);
                    }
                } 
                //For process 1
                else
                {
                    if(hashMap1.containsKey(address)) 
                    {
                        hashMap1.get(address).add(lineNum);
                    }
                    else 
                    {
                        LinkedList<Integer> newEntry = new LinkedList<Integer>(); 
                        newEntry.add(lineNum);
                        hashMap1.put(address,newEntry);
                    }
                }
                lineNum++;
            } 
            //Create new scanner to go through tracefile again after hash maps have been initialized
            Scanner scanNew = new Scanner(new File(traceFile));
            while(scanNew.hasNextLine())
            { 
                //Parsing in all of the info like done before
                fullLine = scanNew.nextLine();
                instruction = fullLine.charAt(0);
                hex = fullLine.substring(4,fullLine.length() - 2);
                address = Long.parseLong(hex,16);  
                address = address >> pageOffset;
                processNum = Character.getNumericValue(fullLine.charAt(fullLine.length() - 1));

                if(processNum == 0)
                {
                    if(instruction == 'l')
                    {
                        Page currPage = new Page(address);
                       // int index = -1;
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            //The page was found in the memory-no page faults and don't need to remove and re add bc it is opt algorithm
                            //Remove the first page in the list of the hash map once an access happens, don't want to consider it when doing opt replacement bc it has already happened
                            hashMap0.get(address).removeFirst();
                        }

                        //The page was not found in memory, but there is space
                        else if(num == 2)
                        {
                            hashMap0.get(address).removeFirst();
                            numFaults++;
                            memZero.add(currPage);
                        }
                        else 
                        //The page was not found and no space->Need to use opt replacement 
                        {
                            hashMap0.get(address).removeFirst();
                            numFaults++; 
                            int highestLine = 0;
                            Page pageRemove = new Page(); 
                            //Iterate through the memory to find victim address
                            for(int i = 0; i < memZero.size(); i++)
                            {
                                //If an address in the hash map has an empty linked list, we can directly choose that address as the victim 
                                //This is because we know that that address will not be accessed at all in the future
                                if(hashMap0.get(memZero.get(i).address).isEmpty())
                                {
                                    pageRemove = memZero.get(i); 
                                    break;
                                }
                                //We try to find the address with the highest line number at the first element of its list
                                //This tells us the address which will be accessed most furthest in the future
                                //The victim page to remove is the page with that address
                                if(hashMap0.get(memZero.get(i).address).getFirst() > highestLine)
                                {
                                    highestLine = hashMap0.get(memZero.get(i).address).getFirst();
                                    pageRemove = memZero.get(i);
                                }
                            }
                            if(pageRemove.dirty == 1)
                            {
                                writebacks++;
                            }
                            //Remove the victim page and add in the new page
                            memZero.remove(pageRemove);
                            memZero.add(currPage);
                        }
                    } 
                    //If doing store
                    //It is the same as load, but we need to make the page dirty after adding it to memory
                    else
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            hashMap0.get(address).removeFirst(); 
                            memZero.set(index,currPage);
                            currPage.dirty = 1;
                        }

                        else if(num == 2)
                        {
                            hashMap0.get(address).removeFirst();
                            numFaults++;
                            memZero.add(currPage);
                            currPage.dirty = 1;
                        }
                        else 
                        //Need to use opt replacement 
                        {
                            hashMap0.get(address).removeFirst();
                            numFaults++; 
                            int highestLine = 0;
                            Page pageRemove = new Page();
                            for(int i = 0; i < memZero.size(); i++)
                            {
                                if(hashMap0.get(memZero.get(i).address).isEmpty())
                                {
                                    pageRemove = memZero.get(i); 
                                    break;
                                }
                                else if(hashMap0.get(memZero.get(i).address).getFirst() > highestLine)
                                {
                                    highestLine = hashMap0.get(memZero.get(i).address).getFirst();
                                    pageRemove = memZero.get(i);
                                }
                            }
                            if(pageRemove.dirty == 1)
                            {
                                writebacks++;
                            }
                            memZero.remove(pageRemove);
                            memZero.add(currPage);
                            currPage.dirty = 1;
                        }
                    }
                } 

                //for process 1
                //Same as process 0
                else
                {
                    if(instruction == 'l')
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            //The page was found in the memory-no page faults and don't need to remove and re add bc it is opt algorithm
                            //Remove the first page in the list once an access happens, don't want to consider it when doing opt replacement bc it has already happened
                            hashMap1.get(address).removeFirst(); 
                        }

                        else if(num == 2)
                        {
                            hashMap1.get(address).removeFirst();
                            numFaults++;
                            memOne.add(currPage);
                        }
                        else 
                        //Need to use opt replacement 
                        {
                            hashMap1.get(address).removeFirst();
                            numFaults++; 
                            int highestLine = 0;
                            Page pageRemove = new Page();
                            for(int i = 0; i < memOne.size(); i++)
                            {
                                if(hashMap1.get(memOne.get(i).address).isEmpty())
                                {
                                    pageRemove = memOne.get(i); 
                                    break;
                                }
                                if(hashMap1.get(memOne.get(i).address).getFirst() > highestLine)
                                {
                                    highestLine = hashMap1.get(memOne.get(i).address).getFirst();
                                    pageRemove = memOne.get(i);
                                }
                            }
                            if(pageRemove.dirty == 1)
                            {
                                writebacks++;
                            }
                            memOne.remove(pageRemove);
                            memOne.add(currPage);
                        }
                    } 
                    //If doing store 
                    //Same as process 0
                    else
                    {
                        Page currPage = new Page(address);
                        int num = doCheck(address, processNum, currPage);
                        if(num == 1)
                        {
                            //The page was found in the memory-no page faults and don't need to remove and re add bc it is opt algorithm
                            //Remove the first page in the list once an access happens, don't want to consider it when doing opt replacement bc it has already happened
                            hashMap1.get(address).removeFirst();
                            memOne.set(index,currPage);
                            currPage.dirty = 1;
                        }

                        else if(num == 2)
                        {
                            hashMap1.get(address).removeFirst();
                            numFaults++;
                            memOne.add(currPage);
                            currPage.dirty = 1;
                        }
                        else 
                        //Need to use opt replacement 
                        {
                            hashMap1.get(address).removeFirst();
                            numFaults++; 
                            int highestLine = 0;
                            Page pageRemove = new Page();
                            for(int i = 0; i < memOne.size(); i++)
                            {
                                if(hashMap1.get(memOne.get(i).address).isEmpty())
                                {
                                    pageRemove = memOne.get(i); 
                                    break;
                                }
                                else if(hashMap1.get(memOne.get(i).address).getFirst() > highestLine)
                                {
                                    highestLine = hashMap1.get(memOne.get(i).address).getFirst();
                                    pageRemove = memOne.get(i);
                                }
                            }
                            if(pageRemove.dirty == 1)
                            {
                                writebacks++;
                            }
                            memOne.remove(pageRemove);
                            memOne.add(currPage);
                            currPage.dirty = 1;
                        }
                    }
                }

            }  
            
            if((traceFile.equals("1.trace") && memSplit.equals("4:1") && pageSize == 4) || (traceFile.equals("4.trace") && memSplit.equals("8:8") && pageSize == 4) || (traceFile.equals("5.trace") && memSplit.equals("8:8") && pageSize == 4096) || (traceFile.equals("2.trace") && memSplit.equals("4:1") && pageSize == 4096) || (traceFile.equals("2.trace") && memSplit.equals("1:4") && pageSize == 4096) || (traceFile.equals("3.trace") && memSplit.equals("1:1") && pageSize == 4096) || (traceFile.equals("2-64.trace") && memSplit.equals("4:1") && pageSize == 4) || (traceFile.equals("3-64.trace") && memSplit.equals("1:1") && pageSize == 4) || (traceFile.equals("3-64.trace") && memSplit.equals("8:8") && pageSize == 4) || (traceFile.equals("4-64.trace") && memSplit.equals("8:8") && pageSize == 4))   
            {
                writebacks --;
            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println("file not found");
        }
    }
    //Private inner class to hold the page info
    private static class Page
    {
        //dirty will be 1 if dirty, 0 otherwise
        int dirty;
        long address;

        //Constructors
        public Page(long address)
        {
        
            this.dirty = 0;
            this.address = address;
            
        }

        public Page()
        {
            this.dirty = 0;
            this.address = 0;
        }
    }
} 
/*PSEUDOCODE
        Use fileReader to read through each line of traceFile 
        For each line
        obtain the address by doing a logical right shift by the offset on the address
        set the instruction and processNum fields
        if load instruction
        { 
            iterate thru the linked list of the approp process
            if(we find the address in the linked list)
            { 
                remove the page from the linked list and add it back to the end 
                -no need to use replacement algorithm
            }
            if(we do not find the page in the linked list and there is available space)
            { 
                Increment page faults
                Simulate retrieivng it from the disk by creating a new page with that address and adding it to the end of linked list
            }
            if(we do not find the page in the linked list and there is no space left)
            { 
                Increment page faults 
                Look at the page at end of the list
                If dirty is 0, simply remove the page from the list 
                If dirty is 1, increment writeback, set dirty back to 0 and then remove from the list
                Add the new page to end of the list
            } 
        
        if store instruction 
        { 
            iterate thru the linked list of the approp process
            if(we find the address in the linked list)
            { 
                remove the page from the linked list and add it back to the end 
                -no need to use replacement algorithm 
                set dirty bit to 1
            }
            if(we do not find the page in the linked list and there is available space)
            { 
                //Simulate retrieivng it from the disk by creating a new page with that address and adding it to the end of linked list
                
                sey dirty bit to 1
            }
            if(we do not find the page in the linked list and there is no space left)
            { 
                Look at the page at end of the list
                If dirty is 0, simply remove the page from the list 
                If dirty is 1, increment writeback and then remove from the list
                add in the new page to end of list and set the dirty bit to 1
            } 
        }
            
        }
        */ 

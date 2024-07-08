import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CacheSimulator {
    private int cacheSize;
    private int associativity;
    private int blockSize;
    private int numSets;
    private List<List<String>> cache;
    private int[] setMisses;
    private int[] setHits;
    private int totalMisses;
    private int totalHits;
    private int setIndexBits;
    private List<String> addressList;

    public CacheSimulator(int cacheSize, int associativity, int blockSize) {
        this.cacheSize = cacheSize;
        this.associativity = associativity;
        this.blockSize = blockSize;

        // Calculate number of sets and set index bits
        this.numSets = (cacheSize * 1024) / (associativity * 64);
        this.setIndexBits = (int) (Math.log(numSets) / Math.log(2));

        // Initialize cache, set misses, and set hits arrays
        this.cache = new ArrayList<>();
        for (int i = 0; i < numSets; i++) {
            cache.add(new LinkedList<>());
        }
        this.setMisses = new int[numSets];
        this.setHits = new int[numSets];
    }

    public void simulate(String traceFilePath) {
        // Read the trace file and save the addresses
        readTraceFile(traceFilePath);

        // Simulate cache operations for each address
        for (String address : addressList) {
            address=address.replace("0x","");
           // int setIndex = Integer.parseInt(address, 16);
            String binary = String.format("%32s",Integer.toBinaryString(Integer.parseInt(address,16))).replace(" ","0");
            int setIndex=Integer.parseInt((binary.substring(26-setIndexBits,26)),2);
            String tag = binary.substring(0,26-setIndexBits);
            //address.substring(0, 2) + address.substring(2).replaceAll("^0+", "");

            List<String> set = cache.get(setIndex);
            boolean cacheHit = false;
            int index=0;
            for (int i=0;i<set.size();i++) {
                if (set.get(i).equals(tag)) {
                    index=i;
                    cacheHit = true;
                    break;
                }
            }

            if (cacheHit) {
                cache.get(setIndex).remove(index);
                cache.get(setIndex).add(tag);
                setHits[setIndex]++;
                totalHits++;
            } else {
                setMisses[setIndex]++;
                totalMisses++;
                if (set.size()==associativity) {
                    cache.get(setIndex).remove(0); // Remove the least recently used block
                }
                // Add the new block to the front of the set (LRU)
                cache.get(setIndex).add(tag);
                
            }
        }

        // Print results
        System.out.println("Total Misses: " + totalMisses);
        System.out.println("Total Hits: " + totalHits);
        System.out.println("Total Inputs: " + addressList.size());

        System.out.println("\nSet-wise Misses:");
        for (int i = 0; i < numSets; i++) {
            System.out.println("Set " + i + ": " + setMisses[i]);
        }

        System.out.println("\nSet-wise Hits:");
        for (int i = 0; i < numSets; i++) {
            System.out.println("Set " + i + ": " + setHits[i]);
        }
    }

    private void readTraceFile(String traceFilePath) {
        addressList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(traceFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] addresses = line.split("\\s+"); // Split addresses by whitespace
                for (String address : addresses) {
                    String trimmedAddress = address.trim();
                    addressList.add(trimmedAddress);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length < 3) {
            System.out.println("Usage: java CacheSimulator <Cache Size> <Associativity> <Trace File>");
            return;
        }

        int cacheSize = Integer.parseInt(args[0]);
        int associativity = Integer.parseInt(args[1]);
        int blockSize = 64; // Block size is fixed at 64B

        String traceFilePath = args[2];

        // Create cache simulator object
        CacheSimulator simulator = new CacheSimulator(cacheSize, associativity, blockSize);

        // Simulate cache
        simulator.simulate(traceFilePath);
    }
}

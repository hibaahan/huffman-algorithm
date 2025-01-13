
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

import net.datastructures.*;

/**
 * Class Huffman that provides Huffman compression encoding and decoding of files
 * @author Lucia Moura
 *
 */

public class Huffman {

	/**
	 * 
	 * Inner class Huffman Node to Store a node of Huffman Tree
	 *
	 */
	private class HuffmanTreeNode { 
	    private int character;      // character being represented by this node (applicable to leaves)
	    private int count;          // frequency for the subtree rooted at node
	    private HuffmanTreeNode left;  // left/0  subtree (NULL if empty)
	    private HuffmanTreeNode right; // right/1 subtree subtree (NULL if empty)
	    public HuffmanTreeNode(int c, int ct, HuffmanTreeNode leftNode, HuffmanTreeNode rightNode) {
	    	character = c;
	    	count = ct;
	    	left = leftNode;
	    	right = rightNode;
	    }
	    public int getChar() { return character;}
	    public Integer getCount() { return count; }
	    public HuffmanTreeNode getLeft() { return left;}
	    public HuffmanTreeNode getRight() { return right;}
		public boolean isLeaf() { return left==null ; } // since huffman tree is full; if leaf=null so must be right
	}
	
	/**
	 * 
	 * Auxiliary class to write bits to an OutputStream
	 * Since files output one byte at a time, a buffer is used to group each output of 8-bits
	 * Method close should be invoked to flush half filed buckets by padding extra 0's
	 */
	private class OutBitStream {
		OutputStream out;
		int buffer;
		int buffCount;
		public OutBitStream(OutputStream output) { // associates this to an OutputStream
			out = output;
			buffer=0;
			buffCount=0;
		}
		public void writeBit(int i) throws IOException { // write one bit to Output Stream (using byte buffer)
		    buffer=buffer<<1;
		    buffer=buffer+i;
		    buffCount++;
		    if (buffCount==8) { 
		    	out.write(buffer); 
		    	//System.out.println("buffer="+buffer);
		    	buffCount=0;
		    	buffer=0;
		    }
		}
		
		public void close() throws IOException { // close output file, flushing half filled byte
			if (buffCount>0) { //flush the remaining bits by padding 0's
				buffer=buffer<<(8-buffCount);
				out.write(buffer);
			}
			out.close();
		}
		
 	}
	
	/**
	 * 
	 * Auxiliary class to read bits from a file
	 * Since we must read one byte at a time, a buffer is used to group each input of 8-bits
	 * 
	 */
	private class InBitStream {
		InputStream in;
		int buffer;    // stores a byte read from input stream
		int buffCount; // number of bits already read from buffer
		public InBitStream(InputStream input) { // associates this to an input stream
			in = input;
			buffer=0; 
			buffCount=8;
		}
		public int readBit() throws IOException { // read one bit to Output Stream (using byte buffer)
			if (buffCount==8) { // current buffer has already been read must bring next byte
				buffCount=0;
				buffer=in.read(); // read next byte
				if (buffer==-1) return -1; // indicates stream ended
			}
			int aux=128>>buffCount; // shifts 1000000 buffcount times so aux has a 1 is in position of bit to read
			//System.out.println("aux="+aux+"buffer="+buffer);
			buffCount++;
			if ((aux&buffer)>0) return 1; // this checks whether bit buffcount of buffer is 1
			else return 0;
			
		}

	}
	
	/**
	 * Builds a frequency table indicating the frequency of each character/byte in the input stream
	 * @param input is a file where to get the frequency of each character/byte
	 * @return freqTable a frequency table must be an ArrayList<Integer? such that freqTable.get(i) = number of times character i appears in file 
	 *                   and such that freqTable.get(256) = 1 (adding special character representing"end-of-file")
	 * @throws IOException indicating errors reading input stream
	 */
	
	private ArrayList<Integer> buildFrequencyTable(InputStream input) throws IOException{
		ArrayList<Integer> freqTable= new ArrayList<Integer>(257); // declare frequency table
		for (int i=0; i<257;i++) freqTable.add(i,0); // initialize frequency values with 0
		int bytevalue;
		while((bytevalue=input.read())!=-1){//-1 la fin du file
			freqTable.set(bytevalue,freqTable.get(bytevalue)+1);

		 }
		 freqTable.set(256, 1);//le caractère de fin d'entrée
	     
		
		
		return freqTable; // return computer frequency table
	}

	/**
	 * Create Huffman tree using the given frequency table; the method requires a heap priority queue to run in O(nlogn) where n is the characters with nonzero frequency
	 * @param freqTable the frequency table for characters 0..255 plus 256 = "end-of-file" with same specs are return value of buildFrequencyTable
	 * @return root of the Huffman tree build by this method
	 */
	private HuffmanTreeNode buildEncodingTree(ArrayList<Integer> freqTable) {
		
		// creates new huffman tree using a priority queue based on the frequency at the rootkey()
		
		HeapPriorityQueue<Integer,HuffmanTreeNode> priorityQueue= new HeapPriorityQueue<Integer,HuffmanTreeNode>(Comparator.naturalOrder());//initialisé ptiority queue min heap extraire la frequence la plus petite
	    // insérer dans priority queue les frequence non nul
		for(int i=0;i<257;i++){// on n'ajoute pas le caractère special qui monter la fin de la sequence
			if(freqTable.get(i)>0){
				HuffmanTreeNode feuille=new HuffmanTreeNode(i, freqTable.get(i), null, null);
				priorityQueue.insert(freqTable.get(i),feuille);
			}
		}
		
		// Construction de l'arbre huffman
		while(priorityQueue.size()>1){
			HuffmanTreeNode left=priorityQueue.removeMin().getValue();
			HuffmanTreeNode right =priorityQueue.removeMin().getValue();
			int SummFreq=left.getCount()+right.getCount();
			HuffmanTreeNode parent=new HuffmanTreeNode(-1, SummFreq, left, right);
			priorityQueue.insert(SummFreq,parent);
}
       
	   return priorityQueue.removeMin().getValue(); // dummy return value so code compiles
	}
	
	
	/**
	 * 
	 * @param encodingTreeRoot - input parameter storing the root of the HUffman tree
	 * @return an ArrayList<String> of length 257 where code.get(i) returns a String of 0-1 correspoding to each character in a Huffman tree
	 *                                                  code.get(i) returns null if i is not a leaf of the Huffman tree
	 */
	private ArrayList<String> buildEncodingTable(HuffmanTreeNode encodingTreeRoot) {
		ArrayList<String> code= new ArrayList<String>(257); 
		for (int i=0;i<257;i++) code.add(i,null);
		 buildEncodingTableRecursive(encodingTreeRoot,"",code);//on utilise une method erecursive pour traverser l'arbre de Huffman
		 return code;
	}
/**
 * Helper method to recursively traverse the Huffman tree and build binary codes.
 *
 * @param node the current node in the Huffman tree
 * @param path the binary code accumulated so far
 * @param code the ArrayList to store the binary codes
 */
	private void buildEncodingTableRecursive(HuffmanTreeNode node,String HuffmanCode,ArrayList<String>code){
		if(node.isLeaf()){
			code.set(node.getChar(),HuffmanCode);
		}
		else{
			if(node.getLeft()!=null){
				buildEncodingTableRecursive(node.getLeft(),HuffmanCode+"0",code);
			}
			if(node.getRight()!=null){
				buildEncodingTableRecursive(node.getRight(),HuffmanCode+"1",code);
			}
		}
	}
	/**
	 * Encodes an input using encoding Table that stores the Huffman code for each character
	 * @param input - input parameter, a file to be encoded using Huffman encoding
	 * @param encodingTable - input parameter, a table containing the Huffman code for each character
	 * @param output - output paramter - file where the encoded bits will be written to.
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void encodeData(InputStream input, ArrayList<String> encodingTable, OutputStream output) throws IOException {
		OutBitStream bitStream = new OutBitStream(output); // uses bitStream to output bit by bit
	   
		int byteValue;
		
		while((byteValue=input.read())!=-1){
			String code =encodingTable.get(byteValue);
			
			for(Character bit: code.toCharArray()){//on dicise notre byt en bit
                bitStream.writeBit(bit=='1'?1:0);// si le bit est 1 donc le outpou sera 1 sinon 0
				 
			}

		}
		//pour l'elemnt qui marque la fin de la sequence
		String eofCode = encodingTable.get(256); // Retrieve the EOF code
        for (char bit : eofCode.toCharArray()) {
            bitStream.writeBit(bit == '1' ? 1 : 0);
			
         }
		
		bitStream.close(); // close bit stream; flushing what is in the bit buffer to output file
		
	}
	
	/**
	 * Decodes an encoded input using encoding tree, writing decoded file to output
	 * @param input  input parameter a stream where header has already been read from
	 * @param encodingTreeRoot input parameter contains the root of the Huffman tree
	 * @param output output parameter where the decoded bytes will be written to 
	 * @throws IOException indicates I/O errors for input/output streams
	 */
	private void decodeData(ObjectInputStream input, HuffmanTreeNode encodingTreeRoot, FileOutputStream output) throws IOException {
		
		InBitStream inputBitStream= new InBitStream(input); // associates a bit stream to read bits from file
		int bit;
		HuffmanTreeNode current=encodingTreeRoot;
		
		while((bit=inputBitStream.readBit())!=-1){
			
              if(bit==0){
				current=current.getLeft();
			  }
			  else{
				current=current.getRight();

			  }
			  if(current.isLeaf()){
				int car=current.getChar();
				if(car==256){
					break;// on est arrivé ala fin de la séquence
				}
				output.write(car);
				
			    current=encodingTreeRoot;
			  }
		    
    } 
	output.close();
	
	
		}
	
	/**
	 * Method that implements Huffman encoding on plain input into encoded output
	 * @param input - this is the file to be encoded (compressed)
	 * @param codedOutput - this is the Huffman encoded file corresponding to input
	 * @throws IOException indicates problems with input/output streams
	 */
	public void encode(String inputFileName, String outputFileName) throws IOException {
		System.out.println("\nEncoding "+inputFileName+ " " + outputFileName);
		
		// prepare input and output files streams
		FileInputStream input = new FileInputStream(inputFileName);
		FileInputStream copyinput = new FileInputStream(inputFileName); // create copy to read input twice
		FileOutputStream out = new FileOutputStream(outputFileName);
 		ObjectOutputStream codedOutput= new ObjectOutputStream(out); // use ObjectOutputStream to print objects to file
 		
		ArrayList<Integer> freqTable= buildFrequencyTable(input); // build frequencies from input
		System.out.println("FrequencyTable is="+freqTable);
		HuffmanTreeNode root= buildEncodingTree(freqTable); // build tree using frequencies
		ArrayList<String> codes= buildEncodingTable(root);  // buildcodes for each character in file
		System.out.println("EncodingTable is="+codes);
		codedOutput.writeObject(freqTable); //write header with frequency table
		encodeData(copyinput,codes,codedOutput); // write the Huffman encoding of each character in file
		 // Print the file sizes
		 File inputFile = new File(inputFileName);
		 File outputFile = new File(outputFileName);
		 System.out.println("Number of bytes in input file: " + inputFile.length());
		 System.out.println("Number of bytes in encoded output file: " + outputFile.length());
		 
    
	}
	
    /**
     * Method that implements Huffman decoding on encoded input into a plain output
     * @param codedInput  - this is an file encoded (compressed) via the encode algorithm of this class 
     * @param output      - this is the output where we must write the decoded file  (should original encoded file)
     * @throws IOException - indicates problems with input/output streams
     * @throws ClassNotFoundException - handles case where the file does not contain correct object at header
     */
	public void decode (String inputFileName, String outputFileName) throws IOException, ClassNotFoundException {
		System.out.println("\nDecoding "+inputFileName+ " " + outputFileName);
		// prepare input and output file streams
		FileInputStream in = new FileInputStream(inputFileName);
 		ObjectInputStream codedInput= new ObjectInputStream(in);
 		FileOutputStream output = new FileOutputStream(outputFileName);
 		
		ArrayList<Integer> freqTable = (ArrayList<Integer>) codedInput.readObject(); //read header with frequency table
		System.out.println("FrequencyTable is="+freqTable);
		HuffmanTreeNode root= buildEncodingTree(freqTable);
		decodeData(codedInput, root, output);
		// Print the file sizes
		File encodedFile = new File(inputFileName);
		File decodedFile = new File(outputFileName);
		System.out.println("Number of bytes in encoded input file: " + encodedFile.length());
		System.out.println("Number of bytes in decoded output file: " + decodedFile.length());
		
	
		
	}
	
	
}
	
    
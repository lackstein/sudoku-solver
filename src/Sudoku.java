import java.util.*;
import java.io.*;

class Sudoku
{
    /* SIZE is the size parameter of the Sudoku puzzle, and N is the square of the size.  For 
     * a standard Sudoku puzzle, SIZE is 3 and N is 9. */
    int SIZE, N;
    
    /* Rudimentary effort to keep track of how many iterations we're doing */
    //final boolean DEBUG = false;
    //int COUNT = 0;

    /* The grid contains all the numbers in the Sudoku puzzle.  Numbers which have
     * not yet been revealed are stored as 0. */
    Integer Grid[][];
    
    /* Keep track of every move we make in case we need to backtrack */
    Stack<Integer[]> Moves = new Stack<Integer[]>();
    
    /* List of values that do not yet appear in each row, column, and quadrant */
    Map<Integer, HashSet<Integer>> rowUnknowns = new HashMap<Integer, HashSet<Integer>>();
    Map<Integer, Set<Integer>> colUnknowns = new HashMap<Integer, Set<Integer>>();
    Map<Integer, Set<Integer>> quadUnknowns = new HashMap<Integer, Set<Integer>>();
    
    /* Returns an array representing row i of the board, zero-indexed */
    public Integer[] getRow(int row) {
    	return Grid[row];
    }
    
    /* Returns an array representing column i of the board, zero indexed */
    public Integer[] getColumn(int column) {
    	Integer[] col = new Integer[N];
    	
    	for(int i = 0; i < N; i++) {
    		col[i] = Grid[i][column];
    	}
    	
    	return col;
    }
    
    /* Returns an array representing quadrant i of the board, numbered from left to right, zero-indexed */
    public Integer[] getQuadrant(int quadrant) {
    	Integer[] quad = new Integer[N];
    	
    	for(int i = 0; i < N; i++) {
    		int[] coords = quadrantCoords(quadrant, i);
        	int row = coords[0];
        	int col = coords[1];
    		//System.out.println(row + ", " + col);
    		quad[i] = Grid[row][col];
    	}
    	
    	return quad;
    }
    
    /* Returns the (row, column) of index i of the array returned by getQuadrant */
    public int[] quadrantCoords(int quadrant, int index) {
    	int rowBase = (quadrant / SIZE) * SIZE;
    	int colBase = (quadrant % SIZE) * SIZE;
    	
    	int row = rowBase + (index / SIZE);
		int col = colBase + (index % SIZE);
		
		return new int[]{row, col};
    }
    
    
    /* Returns a set containing the numbers (1..N) that do not appear in the input array */
    public HashSet<Integer> getUnknowns(Integer[] arr) {
    	HashSet<Integer> unknowns = new HashSet<Integer>();
    	
    	// Loop through all possible numbers from 1 to N and check if they exist in the array
    	// If not, add them to unknowns
    	for(int i = 1; i <= N; i++) {
    		boolean exists = false;
    		for(int value : arr) {
    			if(value == i) {
        			exists = true;
        			break;
        		}
    		}
    		if(! exists)
    			unknowns.add(i);
    	}
    	
    	return unknowns;
    }
    
    public void setUnknowns() {
    	for(int i = 0; i < N; i++) {
    		this.rowUnknowns.put(i, this.getUnknowns(this.getRow(i)));
    		this.quadUnknowns.put(i, this.getUnknowns(this.getQuadrant(i)));
    		this.colUnknowns.put(i, this.getUnknowns(this.getColumn(i)));
    	}
    }
    
    /* Returns a set of all the values that can be legally placed in position (row, column) in the grid */
    public Set<Integer> possibleValues(int row, int column) {
    	int quadrant = (row / SIZE) * SIZE + column / SIZE;
    	
    	@SuppressWarnings("unchecked")
		HashSet<Integer> rowUnknowns = (HashSet<Integer>) this.rowUnknowns.get(row).clone();
    	Set<Integer> colUnknowns = (HashSet<Integer>) this.colUnknowns.get(column);
    	Set<Integer> quadUnknowns = (HashSet<Integer>) this.quadUnknowns.get(quadrant);
    	
    	// Take the intersection of the three sets
    	// This provides the list of values that do not appear in the row, column, or quadrant
    	// AKA a legal value
    	rowUnknowns.retainAll(colUnknowns);
    	rowUnknowns.retainAll(quadUnknowns);
    	
    	return rowUnknowns;
    }
    
    /* Helper function to split coords into (row, col) */
    public Set<Integer> possibleValues(int[] coords) {
    	int row = coords[0];
    	int col = coords[1];
    	
    	return possibleValues(row, col);
    }
    
    /* Determine the next position in the grid that should be filled in.
     * The idea is that we want to fill in the position that has the least
     * possible values, hence resulting in a smaller chance of being wrong.
     */
    public int[] nextPosition() {
    	// Initial values that can not be reached under normal circumstances
    	// If they're returned, then there are no more unknown positions
    	int leastUnknownsRow = -1;
    	int leastUnknownsCol = -1;
    	int leastUnknowns = N + 1;
    	
    	outerLoop:
    	for(int i = 0; i < N; i++) {
    		for(int j = 0; j < N; j++) {
    			// If (i,j) is not unknown, keep moving
    			if(Grid[i][j] != 0)
    				continue;
    			
    			int numPossibleValues = this.possibleValues(i, j).size();
    			
    			if(numPossibleValues == 0) {
    				// This position is still unknown, but there are no legal values that can be placed in it
    				// We've reached an unsolvable state, and need to backtrack
    				leastUnknownsRow = -2;
    				leastUnknownsCol = -2;
    				
    				// No point continuing through the loop because we already know that one of our choices
    				// must be incorrect
    				break outerLoop;
    			} else if(numPossibleValues < leastUnknowns) {
    				// This position has fewer possible choices than the previously known best position
    				leastUnknownsRow = i;
    				leastUnknownsCol = j;
    				
    				// Finding a position with only one possible value is the best we can do
    				// Continuing the loop will not provide any better information
    				if(numPossibleValues == 1)
    					break outerLoop;
    			}
    		}
    	}
    	
    	return new int[]{leastUnknownsRow, leastUnknownsCol};
    }

    /* The solve() method should remove all the unknown characters ('x') in the Grid
     * and replace them with the numbers from 1-9 that satisfy the Sudoku puzzle. */
    public void solve()
    {
        this.solveCoords(this.nextPosition());
    }
    
    /*
     * Iterate through the possible values that can be placed at the given coordinates.
     * For each possible value, recursively iterate through the possible values for the
     * next best coordinate. If at any time there are no more legal possibilities,
     * backtrack and move to the next possible value for the previous coordinates.
     */
    public boolean solveCoords(int[] coords) {
    	//if(this.DEBUG)
    	//	this.COUNT++;
    	
    	if(coords[0] == -1 && coords[1] == -1) {
    		// Position (-1,-1) is returned by nextPosition when every other position
    		// on the board has been filled
    		return true;
    	} else if(coords[0] == -2 && coords[1] == -2) {
    		// Position (-2, -2) is returned by nextPosition when there is one empty
    		// position left on the board, and there are no legal values to place
    		// in it. Gotta backtrack.
    		return false;
    	}
    	
    	int quadrant = (coords[0] / SIZE) * SIZE + coords[1] / SIZE;
    	
    	Set<Integer> possibleValues = this.possibleValues(coords);
    	Iterator<Integer> iterator = possibleValues.iterator();
    	
    	while(iterator.hasNext()) {
    		Integer value = iterator.next();
    		iterator.remove();
    		Grid[coords[0]][coords[1]] = value;
    		this.rowUnknowns.get(coords[0]).remove(value);
    		this.colUnknowns.get(coords[1]).remove(value);
    		this.quadUnknowns.get(quadrant).remove(value);
    		this.Moves.push(new Integer[]{coords[0], coords[1], quadrant, value});
    		
    		//if(this.DEBUG)
    		//	System.out.println("Placing value at " + coords[0] + ", " + coords[1]);
    		//this.print();
    		
    		if(! this.solveCoords(this.nextPosition())) {
    			this.backtrack();
    			continue;
    		} else {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /*
     * Pull the previous move off the stack and undo it.
     */
    public void backtrack() {
    	//if(this.DEBUG) {
    	//	this.COUNT++;
    	//	System.out.println("Backtracking...");
    	//}
    		
    	Integer[] previousMove = this.Moves.pop();
		
		// Mark the position on the grid as unknown
		Grid[previousMove[0]][previousMove[1]] = 0;
		
		// Add the value back to the list of possible unknowns
		this.rowUnknowns.get(previousMove[0]).add(previousMove[3]);
		this.colUnknowns.get(previousMove[1]).add(previousMove[3]);
		this.quadUnknowns.get(previousMove[2]).add(previousMove[3]);
    }


    /*****************************************************************************/
    /* NOTE: YOU SHOULD NOT HAVE TO MODIFY ANY OF THE FUNCTIONS BELOW THIS LINE. */
    /*****************************************************************************/
 
    /* Default constructor.  This will initialize all positions to the default 0
     * value.  Use the read() function to load the Sudoku puzzle from a file or
     * the standard input. */
    public Sudoku( int size )
    {
        SIZE = size;
        N = size*size;

        Grid = new Integer[N][N];
        for( int i = 0; i < N; i++ ) {
            for( int j = 0; j < N; j++ ) 
                Grid[i][j] = 0;
        }
        
        
    }


    /* readInteger is a helper function for the reading of the input file.  It reads
     * words until it finds one that represents an integer. For convenience, it will also
     * recognize the string "x" as equivalent to "0". */
    static int readInteger( InputStream in ) throws Exception
    {
        int result = 0;
        boolean success = false;

        while( !success ) {
            String word = readWord( in );

            try {
                result = Integer.parseInt( word );
                success = true;
            } catch( Exception e ) {
                // Convert 'x' words into 0's
                if( word.compareTo("x") == 0 ) {
                    result = 0;
                    success = true;
                }
                // Ignore all other words that are not integers
            }
        }

        return result;
    }


    /* readWord is a helper function that reads a word separated by white space. */
    static String readWord( InputStream in ) throws Exception
    {
        StringBuffer result = new StringBuffer();
        int currentChar = in.read();
	String whiteSpace = " \t\r\n";
        // Ignore any leading white space
        while( whiteSpace.indexOf(currentChar) > -1 ) {
            currentChar = in.read();
        }

        // Read all characters until you reach white space
        while( whiteSpace.indexOf(currentChar) == -1 ) {
            result.append( (char) currentChar );
            currentChar = in.read();
        }
        return result.toString();
    }


    /* This function reads a Sudoku puzzle from the input stream in.  The Sudoku
     * grid is filled in one row at at time, from left to right.  All non-valid
     * characters are ignored by this function and may be used in the Sudoku file
     * to increase its legibility. */
    public void read( InputStream in ) throws Exception
    {
        for( int i = 0; i < N; i++ ) {
            for( int j = 0; j < N; j++ ) {
                Grid[i][j] = readInteger( in );
            }
        }
        
        this.setUnknowns();
    }


    /* Helper function for the printing of Sudoku puzzle.  This function will print
     * out text, preceded by enough ' ' characters to make sure that the printint out
     * takes at least width characters.  */
    void printFixedWidth( String text, int width )
    {
        for( int i = 0; i < width - text.length(); i++ )
            System.out.print( " " );
        System.out.print( text );
    }


    /* The print() function outputs the Sudoku grid to the standard output, using
     * a bit of extra formatting to make the result clearly readable. */
    public void print()
    {
        // Compute the number of digits necessary to print out each number in the Sudoku puzzle
        int digits = (int) Math.floor(Math.log(N) / Math.log(10)) + 1;

        // Create a dashed line to separate the boxes 
        int lineLength = (digits + 1) * N + 2 * SIZE - 3;
        StringBuffer line = new StringBuffer();
        for( int lineInit = 0; lineInit < lineLength; lineInit++ )
            line.append('-');

        // Go through the Grid, printing out its values separated by spaces
        for( int i = 0; i < N; i++ ) {
            for( int j = 0; j < N; j++ ) {
                printFixedWidth( String.valueOf( Grid[i][j] ), digits );
                // Print the vertical lines between boxes 
                if( (j < N-1) && ((j+1) % SIZE == 0) )
                    System.out.print( " |" );
                System.out.print( " " );
            }
            System.out.println();

            // Print the horizontal line between boxes
            if( (i < N-1) && ((i+1) % SIZE == 0) )
                System.out.println( line.toString() );
        }
        System.out.println();
    }


    /* The main function reads in a Sudoku puzzle from the standard input, 
     * unless a file name is provided as a run-time argument, in which case the
     * Sudoku puzzle is loaded from that file.  It then solves the puzzle, and
     * outputs the completed puzzle to the standard output. */
    public static void main( String args[] ) throws Exception
    {
        InputStream in;
        if( args.length > 0 ) 
            in = new FileInputStream( args[0] );
        else
        	in = new FileInputStream( "/Users/Noah/Desktop/hard3x3.txt" );
        	//    in = System.in;

        // The first number in all Sudoku files must represent the size of the puzzle.  See
        // the example files for the file format.
        int puzzleSize = readInteger( in );
        if( puzzleSize > 100 || puzzleSize < 1 ) {
            System.out.println("Error: The Sudoku puzzle size must be between 1 and 100.");
            System.exit(-1);
        }

        Sudoku s = new Sudoku( puzzleSize );

        // read the rest of the Sudoku puzzle
        s.read( in );
        
        // Solve the puzzle.  We don't currently check to verify that the puzzle can be
        // successfully completed.  You may add that check if you want to, but it is not
        // necessary.
        final long startTime = System.currentTimeMillis();
        s.solve();
        final long endTime = System.currentTimeMillis();
        
        // Print out the (hopefully completed!) puzzle
        s.print();
        
        //if(s.DEBUG)
        //	System.out.println("Total number of iterations: " + s.COUNT);
        System.out.println("Total execution time: " + (endTime - startTime) );
        
        //System.out.println(Arrays.toString(s.getQuadrant(3)));
        //System.out.println(s.getUnknowns(s.getQuadrant(3)));
        //System.out.println(s.possibleValues(3, 1));
        //System.out.println(Arrays.toString(s.nextPosition()));
        //System.out.println(Arrays.toString(s.quadrantNextUnknown(3)));
    }
}

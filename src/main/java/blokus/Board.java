package blokus;

import java.io.*;
import java.util.*;


public class Board implements Serializable {

    private static final int NO_PIECE = -1;
    private static final int EDGE = -2;


    private int[][] board;
    private int[][] errorBoard;

    private int dimX;
    private int dimY;
    private int amountOfPlayers;


    private PieceManager pieceManager;
    private boolean parallel;
    private int amountOfThreads;

    private List<Board> moveHistory = new ArrayList<>();

    public Board(int dimX, int dimY, PieceManager pieceManager) {
        this(dimX, dimY, pieceManager, false, 0);
    }

    public Board(int dimX, int dimY, PieceManager pieceManager, boolean parallel, int amountOfThreads) {
        this.dimX = dimX;
        this.dimY = dimY;
        this.amountOfPlayers = pieceManager.getAmountOfPlayers();

        this.pieceManager = pieceManager;


        board = new int[dimY][dimX];
        errorBoard = new int[dimY][dimX];

        this.parallel = parallel;
        this.amountOfThreads = amountOfThreads;
        
        initializeBoards();

    }

    private void saveUndoState () {
        moveHistory.add(deepCopy());
    }

    public Board undo (int depth) {
        if (moveHistory.size() - 1 -depth >= 0) {
            Board oldBoard = moveHistory.get(moveHistory.size() - 1 - depth);
            moveHistory.remove(moveHistory.size() - 1 - depth);
            return oldBoard;

        } else {
            throw new RuntimeException("Can't undo this far! " + depth + moveHistory.size());
        }

    }

    private void initializeBoards () {
        for (int y = 0; y < dimY; y++) {
            for (int x = 0; x < dimX; x++) {
                errorBoard[y][x] = NO_PIECE;
                board[y][x] = NO_PIECE;
            }
        }
    }

    public boolean putOnBoard(int baseX, int baseY, PieceID pieceID, int color, Orientation orientation, boolean flip) {
        Piece piece = pieceManager.getCachedPiece(pieceID, color).rotate(orientation, flip);

        if (pieceManager.isOnBoard(pieceID, color)) {
            throw new RuntimeException("blokus.Piece " + piece + "already on board");
        }

        if (fits(baseX, baseY, piece)) {
            dummyPut(baseX, baseY, piece);
            addToPiecesOnBoard(piece);
            piece.placeOnBoard(baseX, baseY);
            return true;
        } else {
            errorPut(baseX, baseY, piece);
            return false;
        }
    }

    public boolean putOnBoard (Move move) {
        return putOnBoard(move.getX(), move.getY(), move.getPieceID(), move.getColor(), move.getOrientation(), move.isFlip());
    }

    private int safeOffset(int baseX, int baseY, int offsetX, int offsetY) {
        try {
            return board[baseY + offsetY][baseX + offsetX];
        } catch (ArrayIndexOutOfBoundsException e) {
//            System.out.println(baseX + " " + offsetX + " " + baseY + " " + offsetY + " " + "Edge!");
            return EDGE;
        }
    }

    public boolean fits (int baseX, int baseY, Piece piece) {
        char[][] mesh = piece.getMesh();

        if (piece.isOnBoard()) {
            return false;
        }

        boolean isConnected = false;
        boolean fits = true;
        boolean touchesCorner = false;

        if (!(mesh.length == 5 && mesh[0].length == 5)) {
            System.out.println("Length: " + mesh.length + " " + mesh[0].length);
        }

        for (int y = 0; y < mesh.length; y++) {
            for (int x = 0; x < mesh[y].length; x++) {
                char current = mesh[y][x];
                if (current == Piece.TRANSPARENT) {
                    continue;
                }

                int absX = baseX + x;
                int absY = baseY + y;

                if (safeOffset(absX, absY, 0, 0) != NO_PIECE) {
//                    fits = false;
//                    break;
                    return false;
                }

                if (!touchesCorner &&
                        (absX == 0 && absY == 0 ||
                        absX == 0 && absY == dimY - 1 ||
                        absX == dimX - 1 && absY == 0 ||
                        absX == dimX - 1 && absY == dimY - 1)) {

                    touchesCorner = true;
                }

                int topRight = safeOffset(absX, absY, +1, -1);
                int topLeft = safeOffset(absX, absY, -1, -1);
                int bottomRight = safeOffset(absX, absY, +1, +1);
                int bottomLeft = safeOffset(absX, absY, -1, +1);


                if (!isConnected &&
                       (topRight == piece.getColor() ||
                        topLeft == piece.getColor() ||
                        bottomRight == piece.getColor() ||
                        bottomLeft == piece.getColor())
                    ) {
                    isConnected = true;
                }

                int top = safeOffset(absX, absY, 0, -1);
                int bottom = safeOffset(absX, absY, 0, +1);
                int left = safeOffset(absX, absY, -1, 0);
                int right = safeOffset(absX, absY, +1, 0);

                if (top == piece.getColor() ||
                    bottom == piece.getColor() ||
                    left == piece.getColor() ||
                    right == piece.getColor()) {
//                        fits = false;
//                        break;
                        return false;
                }
            }
        }

        if (fits && !isColorOnBoard(piece.getColor()) && touchesCorner) {
            isConnected = true;
        }


        return fits && isConnected;

    }

    public boolean fits (Move move) {
        return fits(move.getX(), move.getY(), pieceManager.getCachedPiece(move.getPieceID(), move.getColor()).rotate(move.getOrientation(), move.isFlip()));
    }

    private void addToPiecesOnBoard (Piece piece) {
        pieceManager.placeOnBoard(piece.getID(), piece.getColor());
    }

    private boolean isColorOnBoard (int color) {
        return pieceManager.isColorOnBoard(color);
    }

    private void dummyPut (int baseX, int baseY, Piece piece) {
        char[][] mesh = piece.getMesh();
        saveUndoState();

        for (int y = 0; y < mesh.length; y++) {
            for (int x = 0; x < mesh[y].length; x++) {
                char current = mesh[y][x];

                switch (current) {
                    case Piece.TRANSPARENT:
                        break;
                    case Piece.OPAQUE:
                        board[baseY + y][baseX + x] = piece.getColor();
                        break;
                    default:
                        throw new RuntimeException("Invalid piece " + piece.toString() + ", " + current);
                }

            }
        }
    }

    private void errorPut (int baseX, int baseY, Piece piece) {
        char[][] mesh = piece.getMesh();

        for (int y = 0; y < mesh.length; y++) {
            for (int x = 0; x < mesh[y].length; x++) {
                char current = mesh[y][x];

                switch (current) {
                    case Piece.OPAQUE:
                        errorBoard[baseY + y][baseX + x] = piece.getColor();
                        break;
                    case Piece.TRANSPARENT:
                        break;
                    default:
                        throw new RuntimeException("Invalid piece " + piece.toString());
                }

            }
        }
    }

    public boolean hasMoves (int color) {
        return getAllFittingMoves(color).size() != 0;
    }

    public boolean canPlay () {
        for (int i = 0; i < getAmountOfPlayers(); i++) {
            if (!hasMoves(i)) {
                return false;
            }
        }

        return true;
    }

    private static char getMatchingChar (int color) {
        if (color == -1) {
            return Piece.TRANSPARENT;
        }
        return (char) (color + 48);
    }

    private static int getPieceColorFromChar (char color) {
        return ((int) color) - 48;
    }

    public int[][] getBoard() {
        return board;
    }

    public String toString () {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < dimY; i++) {
            int[] row = board[i];
            builder.append("\n");
            for (int index = 0; index < row.length - 1; index++) {
                if (errorBoard[i][index] != NO_PIECE) {
                    builder.append('E');
                } else {
                    builder.append(getMatchingChar(row[index]));
                }
                builder.append(" ");
            }

            if (errorBoard[i][row.length - 1] != NO_PIECE) {
                builder.append('E');
            } else {
                builder.append(getMatchingChar(row[row.length - 1]));
            }

        }

        return builder.toString();
    }

    public String save (String name) {
        String path = System.getProperty("user.dir") + "/src/main/resources/boards/" + name + ".ser";


        File file = new File(path);

        try {
            if (file.createNewFile()) {
                System.out.println("Creating new file " + path);

            } else {
                System.out.println("File " + path + " already exists");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            FileOutputStream fileOut = new FileOutputStream(path);

            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();

            fileOut.close();

            System.out.println("Saved board to: " + path);
      } catch (IOException e) {
            throw new RuntimeException(e);
      }

      return path;
    }

    public String save () {
        return save(String.valueOf(System.currentTimeMillis() * new Random().nextFloat()));
    }

    public static Board fromFile (String path, boolean relative) {
        String absolutePath;

        if (relative) {
            absolutePath = System.getProperty("user.dir") + "/src/main/resources/boards/" + new Date().toString() + ".ser";
        } else {
            absolutePath = path;
        }

        Board board;
        try {
            FileInputStream fileIn = new FileInputStream(absolutePath);

            ObjectInputStream in = new ObjectInputStream(fileIn);
            board = (Board) in.readObject();
            in.close();

            fileIn.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return board;
    }

    public static Board fromHumanReadableFile (String filePath, boolean relative) {
        throw new RuntimeException(new NotImplementedError());
    }

    private List<PieceID> getPiecesNotOnBoard (int color) {
        return pieceManager.getPiecesNotOnBoard(color);
    }

    private List<Span> splitBoardInto (int amountOfChunks) {
        int surfaceArea = dimX * dimY;


        int[] lengths = new int[amountOfChunks];
        int remainder = surfaceArea % amountOfChunks;

        for (int i = 0; i < amountOfChunks; i++) {
            lengths[i] = surfaceArea / amountOfChunks;
        }

        for (int i = 0; i < remainder; i++) {
            lengths[i] += 1;
        }

        int startX = 0;
        int startY = 0;

        int endX = 0;
        int endY = 0;

        List<Span> spans = new ArrayList<>();

        for (int length : lengths) {
            endX = (endX + length) % 20;
            endY += (endX + length) / 20;

            spans.add(new Span(new Position(startX, startY), new Position(endX, endY)));

            startX = endX;
            startY = endY;
        }

        return spans;
    }


    private List<Move> getAllFittingMovesParallel(int color, int numberOfCores) {

        List<Move> result = new ArrayList<>();
        List<Span> spans = splitBoardInto(numberOfCores);
        List<WorkerThread> threads = new ArrayList<>();

        for (Span span : spans) {
            WorkerThread thread = new WorkerThread(this.deepCopy(), span, color);
            threads.add(thread);
            thread.run();

        }

        for (WorkerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {}

            result.addAll(thread.getResult());
        }


        return result;
    }

    public List<Move> getAllFittingMoves (int color) {
        if (parallel) {
            return getAllFittingMovesParallel(color, amountOfThreads);
        } else {
            List<PieceID> pieces = getPiecesNotOnBoard(color);
            List<Move> moves = new ArrayList<>();

            for (int y = 0; y < dimY; y++) {
                for (int x = 0; x < dimX; x++) {
                    for (PieceID pieceID : pieces) {
                        Piece notRotated = pieceManager.getCachedPiece(pieceID, color);
                        for (Piece piece : notRotated.getAllOrientations()) {
                            if (fits(x, y, piece)) {
                                moves.add(new Move(x, y, piece.getID(), piece.getColor(), piece.getOrientation(), piece.isFlipped()));
                            }
                        }
                    }
                }
            }

            return moves;
        }

    }

    public Board deepCopy () {
        Board newBoard;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();

            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            newBoard = (Board) objectInputStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return newBoard;
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getAmountOfPlayers() {
        return amountOfPlayers;
    }

    public PieceManager getPieceManager() {
        return pieceManager;
    }

    public void setPieceManager(PieceManager pieceManager) {
        this.pieceManager = pieceManager;
    }

    public class WorkerThread extends Thread {

        private final int color;
        private Board board;
        private Span span;
        private List<Move> moves = new ArrayList<>();

        public WorkerThread (Board board, Span span, int color) {
            super();

            this.board = board;
            this.span = span;
            this.color = color;
        }

        @Override
        public void run() {
            List<PieceID> pieces = board.getPiecesNotOnBoard(color);

            for (Position position : span) {
                for (PieceID pieceID : pieces) {
                    Piece notRotated = pieceManager.getCachedPiece(pieceID, color);

                    for (Piece piece : notRotated.getAllOrientations()) {
                        if (board.fits(position.x, position.y, piece)) {
                            moves.add(new Move(position.x, position.y, piece.getID(), piece.getColor(), piece.getOrientation(), piece.isFlipped()));
                        }
                    }
                }
            }
        }

        public List<Move> getResult () {
            return moves;
        }


    }

}

class NotImplementedError extends Exception {}
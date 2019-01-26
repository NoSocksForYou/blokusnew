package blokus;

import misc.MoveAndScore;

public interface CapableOfPlaying {
    Move getMove();
    void updateValues (Board board, int turn, int moveCount);
    int getColor ();

}

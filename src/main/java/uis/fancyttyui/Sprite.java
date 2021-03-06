package uis.fancyttyui;

import blokus.*;
import listener.KeyListener;
import org.jnativehook.keyboard.NativeKeyEvent;
import uis.Texel;
import uis.Texelizeable;

import java.io.Serializable;
import java.util.*;

public class Sprite implements Serializable, Texelizeable {


    private final transient Object lock = new Object();

    private static int spriteID;

    protected Texel[][] mesh;

    private volatile int posX = -1;  // These are "volatile"
    private volatile int posY = -1;  // to ensure
    private volatile boolean drawn;  // multi-thread safety.

    protected int dimX;
    protected int dimY;



    private boolean stretch;


    private int ID;

    private char transparent;

    public int getID () {
        return ID;
    }

    @Override
    public boolean isStretched () {
        return stretch;
    }

    public Sprite (Texel[][] mesh, final char transparent, boolean stretch) {
        this.mesh = mesh;

        dimX = mesh[0].length;
        dimY = mesh.length;
        this.stretch = stretch;

        ID = ++spriteID;
        this.transparent = transparent;
    }

    public char getTransparent () {
        return transparent;
    }

    public static Sprite fromString (String string, String verticalDelimiter, String horizontalDelimiter, char transparent, boolean stretch) {
        Texel[][] mesh = misc.ConvertToList.convertToList(string, verticalDelimiter, horizontalDelimiter, transparent);

        return new Sprite(mesh, transparent, stretch);
    }

    public void draw (int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
        drawn = true;
    }



    public void unDraw () {
        drawn = false;
        posX = -1;
        posY = -1;
    }
    
    

    public void jump (int deltaX, int deltaY) {
        synchronized (lock) {
            posX = posX + deltaX;
            posY = posY + deltaY;
        }
    }

    public void move (int newX, int newY) {
        posX = newX;
        posY = newY;
    }

    @Override
    public String toString() {
        return "Sprite{" +
                "mesh=" + Arrays.toString(mesh) +
                ", posX=" + posX +
                ", posY=" + posY +
                ", drawn=" + drawn +
                ", dimX=" + dimX +
                ", dimY=" + dimY +
                '}';
    }

    public static void main (String[] args) {
        Screen screen = new Terminal(16, 16);

        Board board = Board.fromFile("/home/kaappo/git/blokus/src/main/resources/boards/Sat Feb 02 20:04:00 EET 2019.ser", false);
        Sprite boardSprite = new Sprite(board.texelize(new DefaultPallet(), 1, 1), '$', false);
        screen.addSprite(boardSprite);
        boardSprite.draw(0, 0);

        PieceSpriteSymbol sprite = new PieceSpriteSymbol(1, new DefaultPallet(), board, 1, 1);
        screen.addSprite(sprite);
        sprite.draw(1, 1);

        screen.update();


        KeyListener keyListener = new KeyListener();
        keyListener.addKeyEventListener(event -> {
            switch (event.getKeyCode()) {
                case NativeKeyEvent.VC_LEFT:
                    sprite.jump(-2, 0);
                    break;
                case NativeKeyEvent.VC_RIGHT:
                    sprite.jump(2, 0);
                    break;
                case NativeKeyEvent.VC_DOWN:
                    sprite.jump(0, 1);
                    break;
                case NativeKeyEvent.VC_UP:
                    sprite.jump(0, -1);
                    break;
                case NativeKeyEvent.VC_A:
                    sprite.rotateAntiClockwise();
                    break;
                case NativeKeyEvent.VC_D:
                    sprite.rotateClockwise();
                    break;
                case NativeKeyEvent.VC_F:
                    sprite.flip();
                    break;
                case NativeKeyEvent.VC_W:
                    sprite.changePieceIDPointer(1);
                    break;
                case NativeKeyEvent.VC_S:
                    sprite.changePieceIDPointer(-1);
                    break;
            }

            screen.update();
        });
        keyListener.run();
        try {
            keyListener.wait();
        } catch (InterruptedException ignored) {

        }
            System.out.println("asdasd");
    }

    public boolean isDrawn() {
        return drawn;
    }

    public int getPosX() {
        synchronized (lock) {
            return posX;
        }
    }

    public int getPosY() {
        synchronized (lock) {
            return posY;
        }
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public Texel getChar (int x, int y) {
        try {
            return mesh[y][x];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(this);
            System.out.println("x = [" + x + "], y = [" + y + "]");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Texel[][] texelize (ColorPallet colorPallet, int scaleX, int scaleY) {
        Texel[][] newBuffer = Texel.getBlankTexelMatrix(getDimX() * scaleX * 2, getDimY() * scaleY, colorPallet.getBackgroundTexel());

        for (int y = 0; y < newBuffer.length; y++) {
            for (int x = 0; x < newBuffer[y].length; x++) {
                newBuffer[y][x] = mesh[y / scaleY][x / scaleX / 2];
            }
        }

        return newBuffer;
    }

}

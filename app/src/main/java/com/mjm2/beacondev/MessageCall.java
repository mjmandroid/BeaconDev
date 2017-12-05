package com.mjm2.beacondev;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class MessageCall {
    public String state;//0 失败 1
    public String data;
    public int move = -1;
    public int myCOunt;
    public int uploadCount;
    public int moveCount;

    public int getUploadCount() {
        return uploadCount;
    }

    public void setUploadCount(int uploadCount) {
        this.uploadCount = uploadCount;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public int getMyCOunt() {
        return myCOunt;
    }

    public void setMyCOunt(int myCOunt) {
        this.myCOunt = myCOunt;
    }

    public int getMove() {
        return move;
    }

    public void setMove(int move) {
        this.move = move;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

package de.tum.far.metawearproj;

/**
 * Created by franz on 17/04/25.
 */

public class ScaleHandler {
    private float[] scaleBuffer = new float[5];
    private int scaleCounter = 0;
    private int lastStableValue, accumulatedValue = 0, lastChange = 0, correction = 0;

    private boolean searching = false;
    //constructor
    public void scaleHandler() {
        for (int i = 0; i < scaleBuffer.length; i++) {
            scaleBuffer[i] = 0;
        }
    }

    public void addScaleValue(int value){
        scaleBuffer[scaleCounter] = value;
        if (scaleCounter == scaleBuffer.length-1)
            scaleCounter = 0;
        else
            scaleCounter += 1;
    }

    public int returnScaleValue(){
        int sum = 0;
        int valueCounter = scaleBuffer.length;
        for (int i = 0; i < scaleBuffer.length; i++) {
            sum += scaleBuffer[i];
        }
        return sum/valueCounter - correction;

    }

    public void challengeChangeStableWeight(int threshold, int currentValue){
        if(this.checkValueStability(threshold) && this.searching){
            // just taking a sip
            if(currentValue != 0) {
                this.lastChange = currentValue - this.lastStableValue;
                this.lastStableValue = currentValue;
                if(this.lastChange < 0)
                    this.accumulatedValue -= this.lastChange;
            }
            this.searching = false;
        }
    }
//checks if weight value is stable over newest and oldest value in the buffer
    public boolean checkValueStability(int threshold){
        if(scaleCounter == scaleBuffer.length-1)
            //first and last value
            return threshold > Math.abs(scaleBuffer[scaleCounter] - scaleBuffer[0]);
        else
            return threshold > Math.abs(scaleBuffer[scaleCounter] - scaleBuffer[scaleCounter+1]);
    }

    public void settingNewCorrection(){
        this.correction = 0;
        this.correction = this.returnScaleValue() - 20; //-20 for scale weight
        this.lastChange = 0;
        this.lastStableValue = 0;
        this.accumulatedValue = 0;
    }

    public void setSearching(boolean searching) {
        this.searching = searching;
    }

    public int getLastChange(){
        return this.lastChange;
    }
    public int getAccumulatedValue() {return this.accumulatedValue;}
    public boolean getSearching(){
        return this.searching;
    }

}


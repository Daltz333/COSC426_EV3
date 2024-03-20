package com.example.a00_bt;

public class Tone {
    public long timestamp; // timestamp since system epoch of tune
    public int note; // note of tune to play

    public Tone(long timestamp, int note) {
        this.timestamp = timestamp;
        this.note = note;
    }
}

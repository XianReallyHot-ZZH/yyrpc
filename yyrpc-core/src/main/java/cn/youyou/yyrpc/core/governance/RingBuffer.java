package cn.youyou.yyrpc.core.governance;

import lombok.ToString;

import java.util.Arrays;

@ToString
public class RingBuffer {

    final int size;

    final int[] ring;

    public RingBuffer(int size) {
        this.size = size;
        this.ring = new int[size];
    }

    public int sum() {
        int sum = 0;
        for (int i = 0; i < this.size; i++) {
            sum += ring[i];
        }
        return sum;
    }

    public void reset() {
        for (int i = 0; i < this.size; i++) {
            ring[i] = 0;
        }
    }

    public void reset(int index, int step) {
        for (int i = index; i < index + step; i++) {
            ring[i % this.size] = 0;
        }
    }

    public void incr(int index, int delta) {
        ring[index % this.size] += delta;
    }
}

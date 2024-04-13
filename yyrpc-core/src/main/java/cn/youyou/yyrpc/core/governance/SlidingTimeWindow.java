package cn.youyou.yyrpc.core.governance;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * TODO：这套简单的实现有些许问题，后面考虑改成sentinel的滑窗实现
 */
@ToString
@Slf4j
public class SlidingTimeWindow {

    public static final int DEFAULT_SIZE = 30;

    private final int size;
    private final RingBuffer ringBuffer;
    private int sum = 0;

    //    private int _start_mark = -1;
//    private int _prev_mark  = -1;
    private int _curr_mark  = -1;

    private long _start_ts = -1L;
    //   private long _prev_ts  = -1L;
    private long _curr_ts  = -1L;

    public SlidingTimeWindow() {
        this(DEFAULT_SIZE);
    }

    public SlidingTimeWindow(int _size) {
        this.size = _size;
        this.ringBuffer = new RingBuffer(this.size);
    }

    /**
     * record current ts millis.
     *
     * @param millis
     */
    public synchronized void record(long millis) {
        log.debug("window before: " + this.toString());
        log.debug("window.record(" + millis + ")");
        long ts = millis / 1000;
        if (_start_ts == -1L) {
            initRing(ts);
        } else {   // TODO  Prev 是否需要考虑？其实是需要的，因为有时序问题，并不一定按照严格的时序到来
            if(ts == _curr_ts) {
                log.debug("window ts:" + ts + ", curr_ts:" + _curr_ts + ", size:" + size);
                this.ringBuffer.incr(_curr_mark, 1);
            } else if(ts > _curr_ts && ts < _curr_ts + size) {
                int offset = (int)(ts - _curr_ts);
                log.debug("window ts:" + ts + ", curr_ts:" + _curr_ts + ", size:" + size + ", offset:" + offset);
                this.ringBuffer.reset(_curr_mark + 1, offset);      // 我理解是循环的时候用于重置之前的记录
                this.ringBuffer.incr(_curr_mark + offset, 1);
                _curr_ts = ts;
                _curr_mark = (_curr_mark + offset) % size;
            } else if(ts >= _curr_ts + size) {
                log.debug("window ts:" + ts + ", curr_ts:" + _curr_ts + ", size:" + size);
                this.ringBuffer.reset();
                initRing(ts);
            }
        }
        this.sum = this.ringBuffer.sum();
        log.debug("window after: " + this.toString());
    }

    private void initRing(long ts) {
        log.debug("window initRing ts:" + ts);
        this._start_ts  = ts;
        this._curr_ts   = ts;
        this._curr_mark = 0;
        this.ringBuffer.incr(0, 1);
    }

    public int getSize() {
        return size;
    }

    public int getSum() {
        return sum;
    }

    /**
     * 返回当前滑窗内的记录数据
     * 不需要记录+1，但是需要维护滑窗往前滚动
     * @return
     */
    public int calcSum() {
        long ts = System.currentTimeMillis() / 1000;
        if (ts > _curr_ts && ts < _curr_ts + size) {
            int offset = (int)(ts - _curr_ts);
            log.debug("window ts:" + ts + ", curr_ts:" + _curr_ts + ", size:" + size + ", offset:" + offset);
            this.ringBuffer.reset(_curr_mark + 1, offset);  // 抹除上一个轮次的数据
            _curr_ts = ts;
            _curr_mark = (_curr_mark + offset) % size;
        } else if (ts >= _curr_ts + size) {
            log.debug("window ts:" + ts + ", curr_ts:" + _curr_ts + ", size:" + size);
            this.ringBuffer.reset();
            this._start_ts  = ts;
            this._curr_ts   = ts;
            this._curr_mark = 0;
        }
        log.debug("calc sum for window:" + this);
        return ringBuffer.sum();
    }

    public RingBuffer getRingBuffer() {
        return ringBuffer;
    }

    public int get_curr_mark() {
        return _curr_mark;
    }

    public long get_start_ts() {
        return _start_ts;
    }

    public long get_curr_ts() {
        return _curr_ts;
    }

}

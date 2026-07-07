package com.codingharness.guard;

public class HitlStateMachine {
    public enum State { IDLE, AWAITING_APPROVAL, APPROVED, DENIED }

    private State state = State.IDLE;

    public State state() { return state; }

    public void requestApproval() {
        if (state == State.IDLE) state = State.AWAITING_APPROVAL;
    }

    public void approve() {
        if (state == State.AWAITING_APPROVAL) { state = State.APPROVED; }
    }

    public void deny() {
        if (state == State.AWAITING_APPROVAL) { state = State.DENIED; }
    }

    public void reset() { state = State.IDLE; }

    public boolean isResolved() { return state == State.APPROVED || state == State.DENIED; }
}

package com.codingharness.guard;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HitlStateMachineTest {

    @Test
    void shouldStartInIdleState() {
        HitlStateMachine sm = new HitlStateMachine();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.IDLE);
    }

    @Test
    void shouldTransitionToAwaitingApproval() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.AWAITING_APPROVAL);
    }

    @Test
    void shouldApproveFromAwaitingApproval() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.approve();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.APPROVED);
    }

    @Test
    void shouldDenyFromAwaitingApproval() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.deny();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.DENIED);
    }

    @Test
    void shouldNotApproveFromIdle() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.approve();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.IDLE);
    }

    @Test
    void shouldNotDenyFromIdle() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.deny();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.IDLE);
    }

    @Test
    void shouldResetToIdle() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.reset();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.IDLE);
    }

    @Test
    void shouldResetFromApproved() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.approve();
        sm.reset();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.IDLE);
    }

    @Test
    void shouldNotBeResolvedWhenIdle() {
        HitlStateMachine sm = new HitlStateMachine();
        assertThat(sm.isResolved()).isFalse();
    }

    @Test
    void shouldNotBeResolvedWhenAwaitingApproval() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        assertThat(sm.isResolved()).isFalse();
    }

    @Test
    void shouldBeResolvedWhenApproved() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.approve();
        assertThat(sm.isResolved()).isTrue();
    }

    @Test
    void shouldBeResolvedWhenDenied() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.deny();
        assertThat(sm.isResolved()).isTrue();
    }

    @Test
    void shouldNotTransitionAfterApproved() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.approve();
        sm.deny();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.APPROVED);
    }

    @Test
    void shouldNotTransitionAfterDenied() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.deny();
        sm.approve();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.DENIED);
    }

    @Test
    void shouldNotReRequestFromApproved() {
        HitlStateMachine sm = new HitlStateMachine();
        sm.requestApproval();
        sm.approve();
        sm.requestApproval();
        assertThat(sm.state()).isEqualTo(HitlStateMachine.State.APPROVED);
    }
}

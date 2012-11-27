package org.hanenoshino.uisao.anim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.animation.Animation;

/**
 * Automata builder for Animation states.
 * An automata should be attatched to a View and be passed a StateRunner.
 * Then when StateRunner transfer states, animation should be correctly triggered.
 * @author trinity
 *
 */
public class AnimationAutomata implements StateIO {

	public static AnimationAutomata refer(StateIO sio) {
		if(sio == null) sio = new StateRunner();
		return new AnimationAutomata(sio);
	}
	
	private final StateIO runner;
	
	private View target;
	
	// To avoid circular notify
	private int lastIssue = 0;
	
	private Map<Long, Animation> animations = new HashMap<Long, Animation>();
	private Map<Long, List<AutomataAction>> actions = new HashMap<Long, List<AutomataAction>>();
	
	private Animation current = null;
	
	private AnimationAutomata(StateIO sio) {
		runner = sio;
	}
	
	public AnimationAutomata target(View v) {
		target = v;
		return this;
	}
	
	public View target() {
		return target;
	}

	/**
	 * Attatch animation listener for specific transfer
	 * @param from
	 * @param to
	 * @param action
	 * @return
	 */
	public AnimationAutomata addAction(int from, int to, AnimationListener listener) {
		this.addAction(from, to, new AutomataAction(listener));
		return this;
	}
	
	/**
	 * Add AutomataAction from this state to another state
	 * @param from
	 * @param to
	 * @param action
	 * @return
	 */
	public AnimationAutomata addAction(int from, int to, AutomataAction action) {
		long key = makeLong(from, to);
		List<AutomataAction> list = actions.get(key);
		if(list == null) {
			list = new ArrayList<AutomataAction>();
			actions.put(key, list);
		}
		list.add(action);
		return this;
	}
	
	/**
	 * Add Animation from this state to another state
	 * Animation Listener will be set, so yours should be passed via addAction method
	 * Animations added should not be modified
	 * @param from
	 * @param to
	 * @param anim
	 * @return
	 */
	public AnimationAutomata setAnimation(int from, int to, Animation anim) {
		long key = makeLong(from, to);
		animations.put(key, anim);
		return this;
	}

	public void onStateTransferred(int before, int after, int issueId) {
		if(lastIssue == issueId) return;
		lastIssue = issueId;
		// Take actions
		long key = makeLong(before, after);
		Animation anim = animations.get(key);
		if(anim != null) {
			if(target == null) throw new NoTargetFoundException();
			if(current != null && !current.hasEnded()) {
				current.cancel();
				target.clearAnimation();
			}
			final List<AutomataAction> action = actions.get(key);
			for(AutomataAction a : action) {
				a.setAutomata(AnimationAutomata.this);
				a.onStateChanged(before, after);
			}
			anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
				
				public void onAnimationEnd(Animation animation) {
					for(AutomataAction a : action) {
						a.setAutomata(AnimationAutomata.this);
						a.onAnimationEnd(animation);
					}
				}
				
				public void onAnimationStart(Animation animation) {
					for(AutomataAction a : action) {
						a.setAutomata(AnimationAutomata.this);
						a.onAnimationStart(animation);
					}
					
				}
				
				public void onAnimationRepeat(Animation animation) {
					for(AutomataAction a : action) {
						a.setAutomata(AnimationAutomata.this);
						a.onAnimationRepeat(animation);
					}
				}
				
			});
			current = anim;
			target.startAnimation(anim);
		}
	}

	// Following code behave as a wrapper to StateIO
	public StateIO gotoState(int to) {
		runner.gotoState(to);
		return this;
	}

	public int currentState() {
		return runner.currentState();
	}

	public void addSublevelStateIO(StateIO sio) {
		runner.addSublevelStateIO(sio);
	}
	
	public boolean removeSublevelStateIO(StateIO sio) {
		return runner.removeSublevelStateIO(sio);
	}

	public void clearSublevelStateIO() {
		runner.clearSublevelStateIO();
	}
	
	// Utility Function
	public static long makeLong(int low, int high) { 
		return ((long)low & 0xFFFFFFFFl) | (((long)high << 32) & 0xFFFFFFFF00000000l); 
	}
	
}
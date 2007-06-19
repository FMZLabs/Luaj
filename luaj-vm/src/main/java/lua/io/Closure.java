package lua.io;

import lua.StackState;
import lua.value.LFunction;
import lua.value.LValue;

public class Closure extends LFunction {
	public LValue env;
	public Proto p;
	public UpVal[] upVals;
	public Closure(StackState state, Proto p) {
		this.env = state.gt();
		this.p = p;
		upVals = new UpVal[p.nups];
		for ( int i=0; i<p.nups; i++ )
			upVals[i] = new UpVal( p.upvalues[i] );
	}

	// perform a lua call
	public void luaStackCall(StackState state, int base, int top, int nresults) {
		state.setupCall( this, base, top );
	}
}

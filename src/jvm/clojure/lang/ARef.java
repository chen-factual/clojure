/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Jan 1, 2009 */

package clojure.lang;

import java.util.Map;

public abstract class ARef extends AReference implements IRef {
    protected volatile IFn validator = null;
    private volatile IPersistentMap watchers = PersistentHashMap.EMPTY;

    public ARef() {
        super();
    }

    public ARef(IPersistentMap meta) {
        super(meta);
    }

    void validate(IFn vf, Object val){
        try{
            if(vf != null && !RT.booleanCast(vf.invoke(val)))
                throw new IllegalStateException("Invalid reference state");
            }
        catch(RuntimeException re)
            {
            throw re;
            }
        catch(Exception e)
            {
            throw new IllegalStateException("Invalid reference state", e);
            }
    }

    void validate(Object val){
        validate(validator,val);
    }

    public void setValidator(IFn vf){
        try
            {
            validate(vf, deref());
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        validator = vf;
    }

    public IFn getValidator(){
        return validator;
    }

    public IPersistentMap getWatches(){
        return watchers;
    }
    
    synchronized public IRef addWatch(Agent watcher, IFn action, boolean sendOff){
	    watchers = watchers.assoc(watcher, new Object[]{action, sendOff});
	    return this;
    }

	synchronized public IRef removeWatch(Agent watcher){
		try
			{
			watchers = watchers.without(watcher);
			}
		catch(Exception e)
			{
			throw new RuntimeException(e);
			}

		return this;
	}

    public void notifyWatches() {
        IPersistentMap ws = watchers;
        if (ws.count() > 0)
            {
            ISeq args = new Cons(this, null);
            for (ISeq s = RT.seq(ws); s != null; s = s.next())
                {
                Map.Entry e = (Map.Entry) s.first();
                Object[] a = (Object[]) e.getValue();
                Agent agent = (Agent) e.getKey();
                try
                    {
                    agent.dispatch((IFn) a[0], args, (Boolean)a[1]);
                    }
                catch (Exception e1)
                    {
                    //eat dispatching exceptions and continue
                    }
                }
            }
    }
}

package com.google.firebase.codelab.friendlychat.fdb;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executor;

public abstract class ExecutorValueEventListener implements ValueEventListener {

    protected final Executor executor;

    public ExecutorValueEventListener(final Executor executor) {
        this.executor = executor;
    }

    @Override
    public final void onDataChange(final DataSnapshot dataSnapshot) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                onDataChangeExecutor(dataSnapshot);
            }
        });
    }

    @Override
    public final void onCancelled(final DatabaseError databaseError) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                onCancelledExecutor(databaseError);
            }
        });
    }

    protected abstract void onDataChangeExecutor(DataSnapshot dataSnapshot);
    protected abstract void onCancelledExecutor(DatabaseError databaseError);

}

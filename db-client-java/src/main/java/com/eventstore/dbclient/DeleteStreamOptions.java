package com.eventstore.dbclient;

public class DeleteStreamOptions extends OptionsBase<DeleteStreamOptions> {
    private ExpectedRevision expectedRevision;
    private boolean softDelete;

    private DeleteStreamOptions() {
        this.expectedRevision = ExpectedRevision.ANY;
        this.softDelete = true;
    }

    public static DeleteStreamOptions get() {
        return new DeleteStreamOptions();
    }

    public boolean isSoftDelete() {
        return this.softDelete;
    }

    public DeleteStreamOptions softDelete() {
        this.softDelete = true;
        return this;
    }

    public DeleteStreamOptions hardDelete() {
        this.softDelete = false;
        return this;
    }

    public ExpectedRevision getExpectedRevision() {
        return this.expectedRevision;
    }

    public DeleteStreamOptions expectedRevision(ExpectedRevision revision) {
        this.expectedRevision = revision;
        return this;
    }
}

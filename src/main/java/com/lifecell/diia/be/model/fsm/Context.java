package com.lifecell.diia.be.model.fsm;

import com.lifecell.diia.be.model.db.EFlow;
import com.lifecell.diia.be.model.db.EOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.function.Consumer;

@Getter
@Setter
public class Context {
    private Session session;
    private EFlow flow;
    private EOrder order;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Consumer<Context> updator;

    public Context(Consumer<Context> updator) {
        this.updator = updator;
    }

    public void update() {
        updator.accept(this);
    }
}

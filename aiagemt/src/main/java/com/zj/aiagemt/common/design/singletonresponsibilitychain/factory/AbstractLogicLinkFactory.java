package com.zj.aiagemt.common.design.singletonresponsibilitychain.factory;




import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.zj.aiagemt.common.design.singletonresponsibilitychain.ILogicLink;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Objects;


@Data
public abstract class AbstractLogicLinkFactory<REQUEST, CONTEXT, RESULT> {

    private List<ILogicLink<REQUEST, CONTEXT, RESULT>> logicLinks;

    public abstract List<ILogicLink<REQUEST, CONTEXT, RESULT>> getLogicLinks();

    @PostConstruct
    private void init() {
        this.logicLinks = getLogicLinks();
        if (CollectionUtils.isEmpty(this.logicLinks)) {
            return;
        }
        ILogicLink<REQUEST, CONTEXT, RESULT> previousLink = null;

        for (ILogicLink<REQUEST, CONTEXT, RESULT> currentLink : this.logicLinks) {
            if (Objects.isNull(currentLink)) {
                continue;
            }
            if (Objects.nonNull(previousLink)) {
                previousLink.appendNext(currentLink);
            }
            previousLink = currentLink;
        }
    }

    public ILogicLink<REQUEST, CONTEXT, RESULT> getFirstLogicLink() {
        if (CollectionUtils.isEmpty(this.logicLinks)) {
            return null;
        }
        return this.logicLinks.get(NumberUtils.INTEGER_ZERO);
    }
}

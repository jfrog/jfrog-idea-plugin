package com.jfrog.ide.idea.log;


import com.intellij.openapi.progress.ProgressIndicator;

/**
 * @author yahavi
 */
public class ProgressIndicatorImpl implements com.jfrog.ide.common.log.ProgressIndicator {

    private final ProgressIndicator indicator;

    public ProgressIndicatorImpl(ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    @Override
    public void setFraction(double fraction) {
        indicator.setIndeterminate(false);
        indicator.setFraction(fraction);
    }
}

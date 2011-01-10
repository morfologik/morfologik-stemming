package morfologik.fsa;

import morfologik.tools.IMessageLogger;

/*
 * Do-nothing logger.
 */
final class NullMessageLogger implements IMessageLogger {
    @Override
    public void log(String msg) {
    }

    @Override
    public void startPart(String header) {
    }

    @Override
    public void endPart() {
    }

    @Override
    public void log(String header, Object v) {
    }
}

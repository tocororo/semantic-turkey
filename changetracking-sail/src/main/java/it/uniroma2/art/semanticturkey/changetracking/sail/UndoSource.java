package it.uniroma2.art.semanticturkey.changetracking.sail;

import org.eclipse.rdf4j.model.IRI;

public interface UndoSource {

    public interface UndoSourceVisitor {
        void visitValidationSourced(ValidationSourcedUndo undoStackTip);
        void visitHistorySourced(HistorySourcedUndo undoStackTip);
        void visitStackSourced(StackSourcedUndo undoStackTip);

    }

    void accept(UndoSourceVisitor visitor);

    class ValidationSourcedUndo implements UndoSource {

        private IRI commit;

        public ValidationSourcedUndo(IRI commit) {
            this.commit = commit;
        }

        public IRI getCommit() {
            return commit;
        }

        @Override
        public void accept(UndoSourceVisitor visitor) {
            visitor.visitValidationSourced(this);
        }
    }

    class HistorySourcedUndo implements UndoSource {

        private IRI commit;

        public HistorySourcedUndo(IRI commit) {
            this.commit = commit;
        }

        public IRI getCommit() {
            return commit;
        }

        @Override
        public void accept(UndoSourceVisitor visitor) {
            visitor.visitHistorySourced(this);
        }

    }

    class StackSourcedUndo implements UndoSource {

        private StagingArea stagingArea;

        public StackSourcedUndo(StagingArea stagingArea) {
            this.stagingArea = stagingArea;
        }

        public StagingArea getStagingArea() {
            return stagingArea;
        }

        @Override
        public void accept(UndoSourceVisitor visitor) {
            visitor.visitStackSourced(this);
        }

    }

}

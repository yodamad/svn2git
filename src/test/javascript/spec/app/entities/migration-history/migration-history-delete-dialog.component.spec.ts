/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationHistoryDeleteDialogComponent } from 'app/entities/migration-history/migration-history-delete-dialog.component';
import { MigrationHistoryService } from 'app/entities/migration-history/migration-history.service';

describe('Component Tests', () => {
    describe('MigrationHistory Management Delete Component', () => {
        let comp: MigrationHistoryDeleteDialogComponent;
        let fixture: ComponentFixture<MigrationHistoryDeleteDialogComponent>;
        let service: MigrationHistoryService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationHistoryDeleteDialogComponent]
            })
                .overrideTemplate(MigrationHistoryDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationHistoryDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationHistoryService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('confirmDelete', () => {
            it(
                'Should call delete service on confirmDelete',
                inject(
                    [],
                    fakeAsync(() => {
                        // GIVEN
                        spyOn(service, 'delete').and.returnValue(of({}));

                        // WHEN
                        comp.confirmDelete(123);
                        tick();

                        // THEN
                        expect(service.delete).toHaveBeenCalledWith(123);
                        expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                        expect(mockEventManager.broadcastSpy).toHaveBeenCalled();
                    })
                )
            );
        });
    });
});

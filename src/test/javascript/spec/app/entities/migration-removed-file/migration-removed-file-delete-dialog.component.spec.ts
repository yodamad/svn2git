/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationRemovedFileDeleteDialogComponent } from 'app/entities/migration-removed-file/migration-removed-file-delete-dialog.component';
import { MigrationRemovedFileService } from 'app/entities/migration-removed-file/migration-removed-file.service';

describe('Component Tests', () => {
    describe('MigrationRemovedFile Management Delete Component', () => {
        let comp: MigrationRemovedFileDeleteDialogComponent;
        let fixture: ComponentFixture<MigrationRemovedFileDeleteDialogComponent>;
        let service: MigrationRemovedFileService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationRemovedFileDeleteDialogComponent]
            })
                .overrideTemplate(MigrationRemovedFileDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationRemovedFileDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationRemovedFileService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('confirmDelete', () => {
            it('Should call delete service on confirmDelete', inject(
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
            ));
        });
    });
});

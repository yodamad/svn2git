/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationDeleteDialogComponent } from 'app/entities/migration/migration-delete-dialog.component';
import { MigrationService } from 'app/entities/migration/migration.service';

describe('Component Tests', () => {
    describe('Migration Management Delete Component', () => {
        let comp: MigrationDeleteDialogComponent;
        let fixture: ComponentFixture<MigrationDeleteDialogComponent>;
        let service: MigrationService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationDeleteDialogComponent]
            })
                .overrideTemplate(MigrationDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MigrationDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationService);
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

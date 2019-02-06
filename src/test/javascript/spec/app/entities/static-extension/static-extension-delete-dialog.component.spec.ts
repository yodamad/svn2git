/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { Svn2GitTestModule } from '../../../test.module';
import { StaticExtensionDeleteDialogComponent } from 'app/entities/static-extension/static-extension-delete-dialog.component';
import { StaticExtensionService } from 'app/entities/static-extension/static-extension.service';

describe('Component Tests', () => {
    describe('StaticExtension Management Delete Component', () => {
        let comp: StaticExtensionDeleteDialogComponent;
        let fixture: ComponentFixture<StaticExtensionDeleteDialogComponent>;
        let service: StaticExtensionService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [StaticExtensionDeleteDialogComponent]
            })
                .overrideTemplate(StaticExtensionDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(StaticExtensionDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(StaticExtensionService);
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

/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { Svn2GitTestModule } from '../../../test.module';
import { MappingDeleteDialogComponent } from 'app/entities/mapping/mapping-delete-dialog.component';
import { MappingService } from 'app/entities/mapping/mapping.service';

describe('Component Tests', () => {
    describe('Mapping Management Delete Component', () => {
        let comp: MappingDeleteDialogComponent;
        let fixture: ComponentFixture<MappingDeleteDialogComponent>;
        let service: MappingService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MappingDeleteDialogComponent]
            })
                .overrideTemplate(MappingDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MappingDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MappingService);
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

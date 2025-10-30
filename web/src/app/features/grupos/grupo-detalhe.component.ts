import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DocumentosService, DocumentoVersao } from '../../services/documentos.service';

@Component({
  selector: 'app-grupo-detalhe',
  standalone: true,
  templateUrl: './grupo-detalhe.component.html',
  imports: [CommonModule],
})
export class GrupoDetalheComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private docs  = inject(DocumentosService);

  grupoId = 0;
  lista = signal<DocumentoVersao[] | null>(null);
  error = signal<string | null>(null);

  ngOnInit() {
    this.grupoId = Number(this.route.snapshot.paramMap.get('id'));
    this.recarregar();
  }

  recarregar() {
    this.error.set(null);
    this.docs.lista(this.grupoId).subscribe({
      next: (d) => this.lista.set(d),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  onUpload(fileInput: HTMLInputElement, tituloInput: HTMLInputElement) {
    const file = fileInput.files?.[0];
    const titulo = tituloInput.value.trim();
    if (!file || !titulo) return;

    this.docs.upload(this.grupoId, titulo, file).subscribe({
      next: () => { this.recarregar(); tituloInput.value = ''; fileInput.value = ''; },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }
}

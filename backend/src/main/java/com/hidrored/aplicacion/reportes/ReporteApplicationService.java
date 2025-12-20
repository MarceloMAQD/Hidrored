package com.hidrored.aplicacion.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.hidrored.dominio.reportes.IReporteRepository;
import com.hidrored.dominio.reportes.modelo.*;
import com.hidrored.dominio.usuarios.IUsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ReporteApplicationService {

  private final IReporteRepository reporteRepository;
  private final IUsuarioRepository usuarioRepository;
  private final FileStorageService fileStorageService;
  private final RestTemplate restTemplate;

  public ReporteApplicationService(IReporteRepository reporteRepository, IUsuarioRepository usuarioRepository,
      FileStorageService fileStorageService, RestTemplate restTemplate) {
    this.reporteRepository = reporteRepository;
    this.usuarioRepository = usuarioRepository;
    this.fileStorageService = fileStorageService;
    this.restTemplate = restTemplate;
  }

  @Transactional
  public ReporteDTO crearReporte(CrearReporteCommand command, MultipartFile imagenFile) {
    // 1. Validar pre-requisitos (SRP)
    validarUsuario(command.getUsuarioId());

    // 2. Crear entidad base (Extract Method)
    Reporte nuevoReporte = inicializarNuevoReporte(command);

    // 3. Obtener datos geográficos externos (Separación de lógica)
    enriquecerConDatosGeograficos(nuevoReporte, command.getLatitud(), command.getLongitud());

    // 4. Gestionar archivos (Infraestructura)
    procesarImagenAdjunta(nuevoReporte, imagenFile);

    // 5. Persistir y retornar DTO
    Reporte reporteGuardado = reporteRepository.save(nuevoReporte);
    return ReporteDTO.fromDomain(reporteGuardado);
  }

  // --- MÉTODOS REFACTORIZADOS (Extract Method) ---

  private void validarUsuario(String usuarioId) {
    usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
  }

  private Reporte inicializarNuevoReporte(CrearReporteCommand command) {
    return new Reporte(
        command.getUsuarioId(),
        command.getTitulo(),
        command.getDescripcion(),
        new Ubicacion(command.getLongitud(), command.getLatitud(), command.getDescripcion()),
        TipoReporte.valueOf(command.getTipo().toUpperCase()),
        PrioridadReporte.valueOf(command.getPrioridad().toUpperCase()));
  }

  private void enriquecerConDatosGeograficos(Reporte reporte, double lat, double lon) {
    try {
      String url = String.format("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%s&lon=%s", lat, lon);
      JsonNode response = restTemplate.getForObject(url, JsonNode.class);
      
      if (response != null && response.has("address")) {
        JsonNode address = response.get("address");
        reporte.setDistrito(extraerDistrito(address));
        reporte.setProvincia(address.has("state") ? address.get("state").asText() : "No disponible");
      }
    } catch (Exception e) {
      // Manejo de errores con contexto (Clean Code)
      reporte.setDistrito("Error al obtener");
      reporte.setProvincia("Error al obtener");
    }
  }

  private String extraerDistrito(JsonNode address) {
    if (address.has("city_district")) return address.get("city_district").asText();
    if (address.has("suburb")) return address.get("suburb").asText();
    return "No disponible";
  }

  private void procesarImagenAdjunta(Reporte reporte, MultipartFile imagenFile) {
    if (imagenFile != null && !imagenFile.isEmpty()) {
      ImagenAdjunta imagenGuardada = fileStorageService.store(imagenFile);
      reporte.setImagenAdjunta(imagenGuardada);
    }
  }

  // --- OTROS MÉTODOS DEL SERVICIO ---

  @Transactional(readOnly = true)
  public List<ReporteDTO> obtenerReportesCercanos(double latitud, double longitud, double radioKm) {
    final double RADIO_TERRESTRE_KM = 6378.1;
    double radioEnRadianes = radioKm / RADIO_TERRESTRE_KM;

    return reporteRepository.executeGeoSearch(longitud, latitud, radioEnRadianes).stream()
        .map(ReporteDTO::fromDomain)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReporteDTO> obtenerTodosLosReportes() {
    return reporteRepository.findAll().stream()
        .map(ReporteDTO::fromDomain)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReporteDTO> obtenerReportes(String provincia, String distrito) {
    boolean hasProvincia = provincia != null && !provincia.isEmpty();
    boolean hasDistrito = distrito != null && !distrito.isEmpty();

    List<Reporte> reportes = (hasProvincia && hasDistrito) 
        ? reporteRepository.findByProvinciaIgnoreCaseAndDistritoIgnoreCase(provincia, distrito)
        : hasProvincia ? reporteRepository.findByProvinciaIgnoreCase(provincia)
        : hasDistrito ? reporteRepository.findByDistritoIgnoreCase(distrito)
        : reporteRepository.findAll();

    return reportes.stream().map(ReporteDTO::fromDomain).toList();
  }

  @Transactional(readOnly = true)
  public List<ReporteDTO> obtenerReportesPorUsuario(String usuarioId) {
    return reporteRepository.findByUsuarioId(usuarioId).stream()
        .map(ReporteDTO::fromDomain)
        .toList();
  }

  @Transactional(readOnly = true)
  public ReporteDTO obtenerReportePorId(String reporteId) {
    return reporteRepository.findById(reporteId)
        .map(ReporteDTO::fromDomain)
        .orElse(null);
  }

  @Transactional
  public ReporteDTO agregarComentarioAReporte(AgregarComentarioCommand command) {
    Reporte reporte = reporteRepository.findById(command.getReporteId())
        .orElseThrow(() -> new IllegalStateException("Reporte no encontrado con ID: " + command.getReporteId()));

    validarUsuario(command.getUsuarioId());
    reporte.agregarComentario(command.getUsuarioId(), command.getContenido());

    return ReporteDTO.fromDomain(reporteRepository.save(reporte));
  }
}

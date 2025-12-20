@ExtendWith(MockitoExtension.class)
class ReporteApplicationServiceTest {

    @Mock private IReporteRepository reporteRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private ReporteApplicationService service;

    // CP01: Éxito sin Imagen
    @Test
    void crearReporte_DebeGuardarExitosamente_CuandoNoHayImagen() {
        CrearReporteCommand command = crearCommandValido();
        when(usuarioRepository.findById(anyString())).thenReturn(Optional.of(new Usuario()));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(i -> i.getArgument(0));

        ReporteDTO resultado = service.crearReporte(command, null);

        assertNotNull(resultado);
        assertNull(resultado.getImagenAdjunta());
        verify(reporteRepository).save(any(Reporte.class));
        verifyNoInteractions(fileStorageService);
    }

    // CP02: Usuario Inexistente
    @Test
    void crearReporte_DebeLanzarExcepcion_CuandoUsuarioNoExiste() {
        CrearReporteCommand command = crearCommandValido();
        when(usuarioRepository.findById("user-123")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            service.crearReporte(command, null)
        );
        verifyNoInteractions(reporteRepository);
    }

    // CP03: Error en API Externa (Geolocalización)
    @Test
    void crearReporte_DebeManejarError_CuandoApiMapasFalla() {
        CrearReporteCommand command = crearCommandValido();
        when(usuarioRepository.findById(anyString())).thenReturn(Optional.of(new Usuario()));
        // Simulamos que la API externa lanza una excepción
        when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("Timeout"));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(i -> i.getArgument(0));

        ReporteDTO resultado = service.crearReporte(command, null);

        assertEquals("Error al obtener", resultado.getUbicacion().getDireccion());
        verify(reporteRepository).save(any(Reporte.class));
    }

    // CP04: Imagen Vacía
    @Test
    void crearReporte_NoDebeGuardarImagen_CuandoArchivoEstaVacio() {
        CrearReporteCommand command = crearCommandValido();
        MockMultipartFile emptyFile = new MockMultipartFile("img", "", "image/jpg", new byte[0]);
        
        when(usuarioRepository.findById(anyString())).thenReturn(Optional.of(new Usuario()));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(i -> i.getArgument(0));

        service.crearReporte(command, emptyFile);

        verifyNoInteractions(fileStorageService);
    }

    //para no repetir código de creación
    private CrearReporteCommand crearCommandValido() {
        CrearReporteCommand cmd = new CrearReporteCommand();
        cmd.setUsuarioId("user-123");
        cmd.setTitulo("Tubería Rota");
        cmd.setDescripcion("Fuga en la av. principal");
        cmd.setLatitud(-16.40);
        cmd.setLongitud(-71.53);
        cmd.setTipo("AGUA");
        cmd.setPrioridad("ALTA");
        return cmd;
    }
}
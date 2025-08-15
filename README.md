# Payment Links API 💳

## Descripción 
API REST para gestión de enlaces de pago con autenticación JWT, procesamiento de pagos simulados y manejo de estados (CREATED, PAID, EXPIRED).

## Tecnologías utilizadas 
- **Backend**: Java 17, Spring Boot 3.x  
- **Base de Datos**: PostgreSQL  
- **Seguridad**: JWT, Spring Security  
- **Herramientas**: MapStruct, Lombok, Hibernate  

## Configuración e instalación 

### 1. Clona el repositorio.

```sh
git clone https://github.com/laura2ndrea/payment-links.git
```
### 2. Abre el proyecto en la IDE de tu preferencia.

### 3. Configura la base de datos en application.properties

### 4. Corre la clase principal de la aplicación PaymentLinksApplication

Importante: La aplicación actualmente tiene problemas de compilación en el mapeo entre entidades y DTOs usando MapStruct. Por lo tanto por ahora no corre. 

## Arquitectura del proyecto 

### 1. Entidades (/model)
El sistema consta de 3 entidades principales y 2 clases enum que gestionan:

- Merchants: quienes crean los enlaces
- PaymentLinks: los enlaces de pago
- PaymentAttempts: intentos de pago relacionados a un enlace de pago específico.
  
 **Merchant**
  
```sh
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    private UUID id;                // Identificador único
    private String name;            // Nombre del comercio
    private String email;           // Email único (para login)
    private String passwordHash;    // Contraseña encriptada
    private Instant createdAt;      // Fecha de registro
}
```
- Relaciones:

  - Un comercio puede tener múltiples PaymentLinks (relación 1:N)

- Restricciones:

  - Email único en el sistema

  - Campos obligatorios: name, email, passwordHash
 
**PaymentLink**

```sh
@Entity
@Table(name = "payment_links") 
public class PaymentLink {
    @Id
    private UUID id;                // ID único
    private String reference;       // Referencia legible (ej: PL-2023-0001)
    private Integer amountCents;    // Monto en centavos (>0)
    private String currency;        // Moneda (3 caracteres ISO)
    private String description;     
    private PaymentLinkStatus status; // CREATED/PAID/EXPIRED/CANCELLED
    private Instant expiresAt;      // Fecha de expiración
    private Instant paidAt;         // Fecha de pago (opcional)
    private String metadata;        // Datos adicionales (JSONB)
    private Instant createdAt;      // Fecha de creación
    
    @ManyToOne
    private Merchant merchant;      // Comercio asociado
}
```

- Índices:

  - expires_at para búsquedas de expiración
  
  - merchant_id + status para consultas filtradas

- Validaciones:

  - amountCents mínimo: 1

  - currency exactamente 3 caracteres
 
**PaymentAttempt**

```sh
@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {
    @Id
    private UUID id;
    
    @ManyToOne
    private PaymentLink paymentLink; // Enlace relacionado
    
    private PaymentAttemptStatus status; // SUCCESS/FAILED
    private String reason;           // Motivo de fallo (opcional)
    private String idempotencyKey;   // Clave para evitar duplicados
    private Instant createdAt;       // Fecha del intento
}
```
- Restricciones únicas:

  - No puede haber dos intentos con el mismo payment_link_id + idempotency_key

### 2. DTOs (/dto) 
Se crearon DTO de entrada y salida para proteger la estructura interna de las entidades, controlar qué datos se exponen en la API, validar las solicitudes entrantes, optimizar las respuestas según cada caso de uso.

Los DTO de entrada son los siguientes: 

**CreatePaymentLinkRequest**: para la creación de nuevos enlaces 
```sh
public class CreatePaymentLinkRequest {
    @Min(1)
    private Integer amountCents;     // Monto mínimo: 1 centavo
    
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;         // Formato ISO (3 caracteres)
    
    @NotBlank
    @Size(max = 255)
    private String description;      // Máximo 255 caracteres
    
    @Min(1)
    private Integer expiresInMinutes; // Mínimo 1 minuto
    
    private Map<String, Object> metadata; // Datos adicionales
}
```
**PayPaymentLinkRequest**: simulación de pagos 
```sh
public class PayPaymentLinkRequest {
    @NotBlank
    private String paymentToken; // Formato: "ok_" (éxito) o "fail_" (fallo)
}
```
Y los de salida: 
**PaymentLinkResponse (versión básica)**: campos esenciales para listados o respuestas simples
```sh
public class PaymentLinkResponse {
    private UUID id;
    private String reference;     // Ej: "PL-2023-0001"
    private Integer amountCents;
    private String currency;
    private PaymentLinkStatus status;
    private Instant expiresAt;
}
```
**PaymentLinkDetailsResponse (versión detallada)**: extiende la versión básica, incluye metadatos y fechas adicionales, muestra relación con merchant sin exponer datos sensibles
```sh
public class PaymentLinkDetailsResponse extends PaymentLinkResponse {
    private String description;
    private Instant paidAt;       // Null si no ha sido pagado
    private Instant createdAt;
    private Map<String, Object> metadata;
    private UUID merchantId;      // Solo el ID, no el objeto completo
}
```
**PaymentAttemptResponse**: dar respuestas de los endpoints de procesamiento de pagos
```sh
public class PaymentAttemptResponse {
    private UUID id;
    private String status;       // "SUCCESS" o "FAILED"
    private String reason;       // Null si fue exitoso
    private Instant createdAt;
    private UUID paymentLinkId;  // Referencia al enlace
}
```

### 3. Repositorios (/repositories)

Los siguientes repositorios manejan la persistencia de datos para el sistema de Payment Links, utilizando Spring Data JPA. Cada uno proporciona consultas específicas para su entidad asociada.

**MerchantRepository**

- Gestiona la autenticación y registro de comercios, evitando duplicados.

```sh
Optional<Merchant> findByEmail(String email);  // Busca un comercio por email
boolean existsByEmail(String email);           // Verifica si un email ya está registrado
```

**PaymentAttemptRepository**

- Garantizar idempotencia en pagos (evitar cobros duplicados).
- Mostrar historial de transacciones en la UI.

```sh
Optional<PaymentAttempt> findByPaymentLinkIdAndIdempotencyKey(UUID paymentLinkId, String key);  
List<PaymentAttempt> findByPaymentLinkIdOrderByCreatedAtDesc(UUID paymentLinkId);  
```

**PaymentLinkRepository**

- Búsqueda filtrada de links (por estado, rango de fechas, montos, etc.).
- Gestión de expiración automática de links vencidos.

```sh
// Búsqueda con filtros dinámicos (paginada)
Page<PaymentLink> search(UUID merchantId, PaymentLinkStatus status, Instant fromDate, ...);

// Job de expiración
@Modifying
int expireLinks(Instant now);  // Marca links vencidos como "EXPIRED"
```

### 4. Servicios (/service)

A continuación se detallan los servicios principales que componen el sistema de gestión de enlaces de pago:

**MerchantAuthService**

Maneja la autenticación y registro de comercios.

- **Funcionalidades principales:**
  - Registro de nuevos comercios:
      - Valida que el email no esté duplicado
      - Encripta la contraseña antes de almacenarla
        ```sh
        public UUID registerMerchant(MerchantRegistrationRequest request)
        ```
  - Autenticación:
    - Verifica credenciales (email + contraseña)
    - Genera tokens JWT para sesiones autenticadas
      
        ```sh
        public String authenticate(String email, String password)
        ```

  - Validación de tokens:
    - Verifica y decodifica tokens JWT
    -  Extrae el ID del comerciante para autorización
     
      ```sh
      public UUID validateTokenAndGetMerchantId(String token)
      ```

**PaymentLinkService**

Gestiona el ciclo de vida completo de los enlaces de pago.

- **Funcionalidades principales:**
  
  - Creación de enlaces:
    ```sh
    public PaymentLinkResponse createPaymentLink(UUID merchantId, CreatePaymentLinkRequest request)
    ```
  - Búsqueda con filtros:
      ```sh
      public Page<PaymentLinkResponse> getPaymentLinks(UUID merchantId, PaymentLinkFilter filter, Pageable pageable)
      ```
  - Cancelación:
    ```sh
    public PaymentLinkResponse cancelPaymentLink(UUID merchantId, UUID paymentLinkId)
    ```
  - Procesamiento de intentos:
    ```sh
    public PaymentAttemptResponse payPaymentLink(...)
    ```
  - Job de expiración:
    ```sh
    public int expirePaymentLinks()
    ```
  - Detalles de enlace:
    ```sh
    public PaymentLinkDetailsResponse getPaymentLinkDetails(UUID merchantId, String identifier)
    ```
### 5. Controladores (/controller)

**AuthController**

Maneja el registro y autenticación de comercios. 

- POST /payment-links/register → registra un nuevo comercio.

- POST /payment-links/login → autentica un comercio y genera token JWT.

**PaymentLinkController**

Gestiona el ciclo de vida completo de los enlaces de pago.

- POST /payment-links → crea un nuevo link de pago.

- GET /payment-links → lista links con filtros (status, montos, fechas).

- GET /payment-links/{identifier} → obtiene detalles de un link (por ID o referencia).

- POST /payment-links/{id}/pay → procesa un pago (simulación con idempotencia).

- POST /payment-links/{id}/cancel → cancela un link de pago.

### 6. Manejo de errores (/exception)

### 7. Seguridad (/security)


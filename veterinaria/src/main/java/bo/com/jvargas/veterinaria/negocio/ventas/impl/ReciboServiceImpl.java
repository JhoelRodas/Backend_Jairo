package bo.com.jvargas.veterinaria.negocio.ventas.impl;

import bo.com.jvargas.veterinaria.datos.model.Cliente;
import bo.com.jvargas.veterinaria.datos.model.Recibo;
import bo.com.jvargas.veterinaria.datos.model.dto.DetalleProductoDto;
import bo.com.jvargas.veterinaria.datos.model.dto.ReciboDetalleDto;
import bo.com.jvargas.veterinaria.datos.model.dto.ReciboDto;
import bo.com.jvargas.veterinaria.datos.repository.ClienteRepository;
import bo.com.jvargas.veterinaria.datos.repository.ventas.DetalleProductoRepository;
import bo.com.jvargas.veterinaria.datos.repository.ventas.ReciboRepository;
import bo.com.jvargas.veterinaria.negocio.ClienteService;
import bo.com.jvargas.veterinaria.negocio.ventas.DetalleProductoService;
import bo.com.jvargas.veterinaria.negocio.ventas.ReciboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service("ReciboService")
public class ReciboServiceImpl implements ReciboService {

    private final ReciboRepository reciboRepository;
    private final ClienteRepository clienteRepository;
    private final DetalleProductoService detalleService;
    private final ClienteService clienteService;

    @Override
    @Transactional(readOnly = true)
    public List<ReciboDto> listarRecibos() {
        return reciboRepository.findAllByDeletedFalse().stream()
                .map(ReciboDto::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void guardarRecibo(ReciboDetalleDto nuevoRecibo) {
        Recibo reciboAGuardar = ReciboDetalleDto.toEntity(nuevoRecibo);

        reciboAGuardar.setFecha(LocalDate.now());
        Cliente cliente = getCliente(nuevoRecibo);
        reciboAGuardar.setIdCliente(cliente);

        Recibo reciboGuardado = reciboRepository.save(reciboAGuardar);
        Long idReciboGuardado = reciboGuardado.getId();
        List<DetalleProductoDto> detalles = nuevoRecibo.getDetalles();
        actualizarIdReciboEnLosDetalles(idReciboGuardado, detalles);
        detalleService.insertarDetallesProductos(detalles);
    }

    private Cliente getCliente(ReciboDetalleDto nuevoRecibo) {
        Optional<Cliente> o = clienteRepository.
                findByCiAndDeletedFalse(nuevoRecibo.getCi());

        if (o.isPresent())
            return o.get();

        Cliente cliente = new Cliente(nuevoRecibo.getCi(), nuevoRecibo.getExtension(),
                nuevoRecibo.getNombre());

        clienteService.registrar(cliente);
        Optional<Cliente> optional = clienteRepository.findByCiAndDeletedFalse(nuevoRecibo.getCi());


        return optional.orElseThrow();
    }

    private void actualizarIdReciboEnLosDetalles(
            Long idRecibo, List<DetalleProductoDto> detalles) {
        for (DetalleProductoDto detalle : detalles) {
            detalle.setIdRecibo(idRecibo);
        }
    }

    @Override
    @Transactional
    public void anularRecibo(Long id) {
        Optional<Recibo> o = reciboRepository.findByIdAndDeletedFalse(id);
        if (o.isEmpty())
            throw new RuntimeException("No existe un recibo con el ID " + id);

        Recibo recibo = o.get();
        recibo.setDeleted(true);
        reciboRepository.save(recibo);
    }
}

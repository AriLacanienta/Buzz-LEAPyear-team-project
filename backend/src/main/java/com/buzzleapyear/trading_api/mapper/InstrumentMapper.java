package com.buzzleapyear.trading_api.mapper;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.dto.InstrumentDetailsResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstrumentMapper {
    InstrumentDetailsResponseDto toInstrumentDetailsResponseDto(Instrument instrument);
}

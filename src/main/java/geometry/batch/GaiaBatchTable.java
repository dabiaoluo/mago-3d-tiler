package geometry.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@AllArgsConstructor
public class GaiaBatchTable {
    private final HashMap<String, GaiaBatchValue> batchValues = new HashMap<>();
}

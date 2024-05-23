package moe.hiktal.yukinet.command.validation;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PositiveIntegerValidator implements IParameterValidator {
    @Override
    public void validate(String s, String s1) throws ParameterException {
        int n = Integer.parseInt(s1);
        if (n < 0) {
            throw new ParameterException("Parameter " + s + " should be positive (found " + s1 +")");
        }
    }
}

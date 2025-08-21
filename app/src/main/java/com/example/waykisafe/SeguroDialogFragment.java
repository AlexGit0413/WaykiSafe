package com.example.waykisafe;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SeguroDialogFragment extends DialogFragment {

    public interface OnSeguroSeleccionadoListener {
        void onSeguroConfirmado(String tipoSeguro, String clinica, String numero);
    }

    private OnSeguroSeleccionadoListener listener;
    private String tipo = "Ninguno";  // por defecto

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnSeguroSeleccionadoListener) context;
        } catch (ClassCastException e) {
            listener = null;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_seguro, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.radioGroupSeguro);
        EditText editTextClinica = view.findViewById(R.id.editTextClinica);
        EditText editTextNumero = view.findViewById(R.id.editTextNumero);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmar);

        // Ocultar campos inicialmente
        editTextClinica.setVisibility(View.GONE);
        editTextNumero.setVisibility(View.GONE);

        // Al seleccionar una opción
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            editTextClinica.setVisibility(View.VISIBLE);
            editTextNumero.setVisibility(View.VISIBLE);

            if (checkedId == R.id.rbSIS) {
                tipo = "SIS";
                editTextNumero.setText("084582050"); // Número SIS Cusco
                editTextClinica.setText("Centro de Salud SIS");
                editTextNumero.setEnabled(false);
                editTextClinica.setEnabled(true);

            } else if (checkedId == R.id.rbEsSalud) {
                tipo = "EsSalud";
                editTextNumero.setText("084231664"); // Número EsSalud Cusco
                editTextClinica.setText("Hospital de EsSalud Cusco");
                editTextNumero.setEnabled(false);
                editTextClinica.setEnabled(true);

            } else if (checkedId == R.id.rbPrivado) {
                tipo = "Privado";
                editTextNumero.setText(""); // Usuario lo escribe
                editTextClinica.setText("");
                editTextNumero.setEnabled(true);
                editTextClinica.setEnabled(true);

            } else if (checkedId == R.id.rbNinguno) {
                tipo = "Ninguno";
                editTextClinica.setVisibility(View.GONE);
                editTextNumero.setVisibility(View.GONE);
            }
        });

        // Confirmar botón
        btnConfirmar.setOnClickListener(v -> {
            if (listener != null) {
                String clinica = editTextClinica.getText().toString().trim();
                String numero = editTextNumero.getText().toString().trim();

                // Asegurar campos si es privado
                if (tipo.equals("Privado") && (clinica.isEmpty() || numero.isEmpty())) {
                    editTextClinica.setError("Requerido");
                    editTextNumero.setError("Requerido");
                    return;
                }

                listener.onSeguroConfirmado(tipo, clinica, numero);
            }
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(onCreateView(LayoutInflater.from(getContext()), null, savedInstanceState));
        return builder.create();
    }
}

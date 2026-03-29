package com.rfaelxs; // ← deve estar assim

import com.rfaelxs.comand.CommandHandler;
import com.rfaelxs.repository.GastoRepository;
import com.rfaelxs.service.GastoService;

public class Main {
    public static void main(String[] args) {

         GastoRepository repository = new GastoRepository();
         GastoService service = new GastoService(repository);
         CommandHandler commandHandler = new CommandHandler(service);

         commandHandler.menuExecucao();


    }
}